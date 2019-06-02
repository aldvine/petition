package foo;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
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
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;


@Api(name = "myApi",
version = "v1",
namespace = @ApiNamespace(ownerDomain = "helloworld.example.com",
    ownerName = "helloworld.example.com",
    packagePath = ""))

public class PetitionEndpoint {
	
	// authentification avec google simple https://cloud.google.com/java/getting-started-appengine-standard/authenticate-users?hl=fr#understanding_the_code
	
	@ApiMethod(name = "addPetition",
			httpMethod = ApiMethod.HttpMethod.POST, path="petition")
	public Entity addPetition(Petition petition) {
//		petition.title = "test";
//		petition.description = "test description";
//		petition.creator = "creator";
			if(petition !=null) {
				if(petition.testValid()) {
					
					DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
					Entity pet = new Entity("Petition", petition.title.trim()); // TODO verifier si elle n'existe pas déjà 
					pet.setProperty("title", petition.title);
					pet.setProperty("description", petition.description);
					// premier signataires = le createur
					pet.setProperty("counter", 1);
					String lastIndexOfSignatures = petition.title.trim()+"_start-1";
					pet.setProperty("signaturesIndex",lastIndexOfSignatures);
					Entity signatures = new Entity("Signatures",lastIndexOfSignatures,pet.getKey()); 
					ArrayList<String> signatories = new ArrayList<String>();
					// ajout du créateur dans les signatures
					signatories.add(petition.creator);
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
	public List<Entity> getAllPetitions() {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition")
				.addProjection(new PropertyProjection("title", String.class))
				.addProjection(new PropertyProjection("description", String.class))
				.addProjection(new PropertyProjection("counter", Long.class));
		PreparedQuery pq = datastore.prepare(q);
		return pq.asList(FetchOptions.Builder.withDefaults());
	}
	
	@ApiMethod(name = "getTopPetitions", httpMethod = ApiMethod.HttpMethod.GET, path="petition/top")
	public List<Entity> getTopPetitions() {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Petition")
				.addProjection(new PropertyProjection("title", String.class))
				.addProjection(new PropertyProjection("description", String.class))
				.addProjection(new PropertyProjection("counter", Long.class))
				.addSort("counter", SortDirection.DESCENDING);
		PreparedQuery pq = datastore.prepare(q);
		return pq.asList(FetchOptions.Builder.withLimit(100));
	}
	
	@SuppressWarnings("unchecked")
	@ApiMethod(name = "signPetition",httpMethod = ApiMethod.HttpMethod.POST,path="signature")
	public Response addSignatory(Signature signature) throws EntityNotFoundException {
		// retour

		Response response = new Response();
		response.code="500";
		response.message = "Un problème est survenue lors de la signature (veuillez contacter un administateur)";
		signature.title = "test";
		signature.signatory="bonjour";
		if(signature.testValid()) {
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Filter f = new FilterPredicate("signatories", FilterOperator.EQUAL, signature.signatory);
			// Limiter la requete sur des signatures à l'ancetre Petition
			// https://cloud.google.com/appengine/docs/standard/java/datastore/queries#ancestor_filters
			Key petKey = KeyFactory.createKey("Petition", signature.title.trim());
			Query q = new Query("Signatures").setFilter(f).setAncestor(petKey);
			PreparedQuery pq = datastore.prepare(q);
			Boolean isNotSigned =  pq.asList(FetchOptions.Builder.withLimit(1)).isEmpty();
			if(isNotSigned) {			
				Entity petition = datastore.get(petKey);
				// aide https://cloud.google.com/appengine/docs/standard/java/datastore/transactions
				Transaction txn = datastore.beginTransaction();
				try {
					Key signaturesKey = KeyFactory.createKey(petKey,"Signatures",petition.getProperty("signaturesIndex").toString());
					
					Entity signatures = datastore.get(signaturesKey);

					int total = Integer.parseInt(signatures.getProperty("total").toString());
					int counter = Integer.parseInt(petition.getProperty("counter").toString());
					counter++;
					
					petition.setProperty("counter",counter);
					if(total>=5000) {
						// changement de liste carte limite atteinte
						
						String indexOfSignatures = petition.getProperty("title").toString()+"_start-"+total;
						
						Entity newSignatures =  new Entity("Signatures",indexOfSignatures,petition.getKey()); 
						total =1;
						signatures.setProperty("total", 1);
						ArrayList<String> signatories = new ArrayList<String>();
						// ajout du signataires dans la liste des signatures
						signatories.add(signature.signatory);
						signatures.setProperty("signatories", signatories );
						// ajouter +1 au compteur global. 
						petition.setProperty("signaturesIndex",indexOfSignatures);
						// changer l'index Vers la liste des signatures actuelle pour la pétition
							
					}else {
						total++;
						// récuperer la liste des signataires, ajouter le signatire dans cette liste puis mettre à jour la liste 
						signatures.setProperty("total", total);
						ArrayList<String> signatories ;
						signatories = (ArrayList<String>) signatures.getProperty("signatories");
						signatories.add(signature.signatory);
						signatures.setProperty("signatories", signatories);
					}
								
					//  ajouter dans le datastore la petition modifié et l'index liste des signataires OK
					datastore.put(txn, petition);
					datastore.put(txn, signatures);		
					txn.commit();
					response.code="200";
					response.message = "La signature a bien été prise en compte.";
				} finally {
				  if (txn.isActive()) {
				    txn.rollback();
				  }
				}
			}else {
				response.code="403";
				response.message = "La signature a déjà été prise en compte.";
			}
		}
		// pour tester sans IHM
//		signature.signatory="test@test.fr";
//		signature.title = "test";
		
		
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