package foo;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;

import com.google.api.server.spi.auth.common.User;


@Api(name = "myApi",
version = "v1",
namespace = @ApiNamespace(ownerDomain = "helloworld.example.com",
    ownerName = "helloworld.example.com",
    packagePath = ""))

public class PetitionEndpoint {
	
	final static String clientsIds = "122644336388-92j45odst8ekhc91lhtj49rv4ipr8q61.apps.googleusercontent.com";
	// authentification avec google sign in https://cloud.google.com/endpoints/docs/frameworks/java/javascript-client?hl=fr
	
//	@SuppressWarnings("unchecked")
	@ApiMethod(name = "addPetition",
			httpMethod = ApiMethod.HttpMethod.POST, path="petition",
			clientIds = {PetitionEndpoint.clientsIds},
			   audiences = {PetitionEndpoint.clientsIds})
	public Entity addPetition(User user,Petition petition) throws UnauthorizedException, EntityNotFoundException {
		
		 if (user == null) {
			    throw new UnauthorizedException("Invalid credentials");
		 }

			if(petition !=null) {
				if(petition.testValid()) {
					
					DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
					Entity pet = new Entity("Petition", petition.title.trim().replaceAll("\\s+","_")); // TODO verifier si elle n'existe pas déjà sinon elle va etre remplacé avec plus de signataires
					
					pet.setProperty("title", petition.title);
					pet.setProperty("description", petition.description);
					// premier signataires = le createur
					pet.setProperty("counter", 1);
					pet.setProperty("creator", user.getEmail());
					String lastIndexOfSignatures = petition.title.trim().replaceAll("\\s+","_")+"_start-1";
					pet.setProperty("signaturesIndex",lastIndexOfSignatures);
					Entity signatures = new Entity("Signatures",lastIndexOfSignatures,pet.getKey()); 
					ArrayList<String> signatories = new ArrayList<String>();
					// ajout du créateur dans les signatures
					signatories.add(user.getEmail());
					signatures.setProperty("signatories", signatories );
					signatures.setProperty("total", 1);
					
					datastore.put(pet);
					datastore.put(signatures);

					return pet;
				}else {
					return null;
				}
			}else {
				return null;
			}
	}
	
	@ApiMethod(name = "getAllPetitions", httpMethod = ApiMethod.HttpMethod.GET, path="petitions")
	public List<Entity> getAllPetitions(@Nullable @Named("next") String lastkey) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition");

		if(lastkey != null) {
			Key petKey = KeyFactory.createKey("Petition", lastkey.trim().replaceAll("\\s+","_"));
			Filter filterPetition = new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN, petKey);
			q.setFilter(filterPetition);
		}
		
		PreparedQuery pq = datastore.prepare(q);
		return pq.asList(FetchOptions.Builder.withLimit(10));
	}
		
	@ApiMethod(name = "getTopPetitions", httpMethod = ApiMethod.HttpMethod.GET, path="petition/top")
	public List<Entity> getTopPetitions() {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition").addSort("counter", SortDirection.DESCENDING);
		PreparedQuery pq = datastore.prepare(q);
		return pq.asList(FetchOptions.Builder.withLimit(100));
	}
	
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "signPetition",httpMethod = ApiMethod.HttpMethod.POST,path="signature",
			clientIds = {PetitionEndpoint.clientsIds},
			   audiences = {PetitionEndpoint.clientsIds})
	public Response addSignatory(User user,Signature signature) throws EntityNotFoundException,UnauthorizedException {
		 
		if (user == null) {
			    throw new UnauthorizedException("Invalid credentials");
		 }

		Response response = new Response();
		response.code="500";
		response.message = "Un problème est survenue lors de la signature (veuillez contacter un administateur)";

		if(signature.testValid()) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Filter f = new FilterPredicate("signatories", FilterOperator.EQUAL, user.getEmail());
			// Limiter la requete sur des signatures à l'ancetre Petition
			// https://cloud.google.com/appengine/docs/standard/java/datastore/queries#ancestor_filters
			Key petKey = KeyFactory.createKey("Petition", signature.title.trim().replaceAll("\\s+","_"));
			Query q = new Query("Signatures").setAncestor(petKey).setFilter(f);
			// la transaction est lancé avant la requête pour vérifier si l'utilisateur a déja signé sinon il peut signer deux fois si l'action est lancée en parallèle au même moment. 
			Transaction txn = datastore.beginTransaction();
			try {
				PreparedQuery pq = datastore.prepare(q);
				Boolean isNotSigned =  pq.asList(FetchOptions.Builder.withLimit(1)).isEmpty();
				if(isNotSigned) {			
					Entity petition = datastore.get(petKey);
					// aide https://cloud.google.com/appengine/docs/standard/java/datastore/transactions
					
						Key signaturesKey = KeyFactory.createKey(petKey,"Signatures",petition.getProperty("signaturesIndex").toString());
						
						Entity signatures = datastore.get(signaturesKey);
	
						int total = Integer.parseInt(signatures.getProperty("total").toString());
						int counter = Integer.parseInt(petition.getProperty("counter").toString());
						// ajouter +1 au compteur global. 
						counter++;
						petition.setProperty("counter",counter);
						if(total>=5000) {
							// changement de liste car limite de taille de liste atteinte (voir diapo)
							
							String indexOfSignatures = petition.getProperty("title").toString()+"_start-"+total;
							
						    signatures =  new Entity("Signatures",indexOfSignatures,petition.getKey()); 
							total =1;
							signatures.setProperty("total", 1);
							ArrayList<String> signatories = new ArrayList<String>();
							// ajout du signataires dans la liste des signatures
							signatories.add(user.getEmail());
							signatures.setProperty("signatories", signatories );
							
							petition.setProperty("signaturesIndex",indexOfSignatures);
							// changer l'index Vers la liste des signatures actuelle pour la pétition
								
						}else {
							total++;
							// récuperer la liste des signataires, ajouter le signatire dans cette liste puis mettre à jour la liste 
							signatures.setProperty("total", total);
							ArrayList<String> signatories ;
							signatories = (ArrayList<String>) signatures.getProperty("signatories");
							signatories.add(user.getEmail());
							signatures.setProperty("signatories", signatories);
						}
						
						
						//  ajouter dans le datastore la petition modifié et l'index liste des signataires OK
						datastore.put(txn, petition);
						datastore.put(txn, signatures);	
						
						
						response.code="200";
						response.message = "La signature a bien été prise en compte.";
					
				}else {
					response.code="403";
					response.message = "La signature a déjà été prise en compte.";
				}
				txn.commit();
			} finally {
				  if (txn.isActive()) {
				    txn.rollback();
				  }
			}
		}		
		return response;
	}
	
	@ApiMethod(name = "getPetitionsSignedByUser", httpMethod = ApiMethod.HttpMethod.GET, path="user/{username}/signatures")
	public List<Entity> getPetitionsSignedByUser(@Named("username") String username) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Filter f = new FilterPredicate("signatories", FilterOperator.EQUAL, username);
		Query q = new Query("Signatures").setFilter(f).setKeysOnly();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> signatures = pq.asList(FetchOptions.Builder.withDefaults());
		
		List<Key> pets = new ArrayList<Key>();

		for (Entity s : signatures) {
			pets.add(s.getParent());
		}
		Map<Key,Entity> petitionsMap = datastore.get(pets);	
		List<Entity> petitions = new ArrayList<Entity>(petitionsMap.values());
		return petitions;
	}
	
}