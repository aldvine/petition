package foo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

@WebServlet(name = "PetitionServlet", urlPatterns = { "/populate" })
public class PetitionServlet extends HttpServlet {

	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().print("Populating ... <br>");
		Random r = new Random();
		ArrayList<Entity> petitionList = new ArrayList<Entity>();
		ArrayList<Entity> signaturesList = new ArrayList<Entity>();
		// Create users
		int numberOfPetitions = r.nextInt(7); // création entre 1 et 7 pétitions (Si plus risque -> TIME OUT)
		for (int i = 0; i < numberOfPetitions; i++) {
			int petitionNumber = r.nextInt(10000); // nombre aléatoire de pétition
			Entity pet = new Entity("Petition", "petition_"+petitionNumber); // TODO verifier si elle n'existe pas déjà sinon elle va etre remplacé avec plus de signataires
			
			pet.setProperty("title", "petition "+petitionNumber);
			pet.setProperty("description", "lorem ipsum "+petitionNumber);
			int counter = 1;
			pet.setProperty("counter", counter);
			String creator = "u"+r.nextInt(10000);
			pet.setProperty("creator", creator);
			String lastIndexOfSignatures = "petition_"+petitionNumber+"_start-1";
			pet.setProperty("signaturesIndex",lastIndexOfSignatures);
			Entity signatures = new Entity("Signatures",lastIndexOfSignatures,pet.getKey()); 
			HashSet<String> signatories = new HashSet<String>();
			// ajout du créateur dans les signatures
			signatories.add(creator);
			signatures.setProperty("signatories", signatories );
			int total = 1;
			signatures.setProperty("total", total);

			response.getWriter().print("<li> creation of Petition:" + pet.getKey() + "<br>");
			int numberOfSignatories = r.nextInt(6000); // création entre 1 et 6000 petition pour tester le principe de changement de liste si le nombre de 5000 est atteint
			for (int j = 0; j < numberOfSignatories; j++) {
				
				int oldSizeOfList = signatories.size();
				// tant que l'utilisateur ajouté n'est pas nouveau
				while(signatories.size() <= oldSizeOfList) {
					signatories.add("u" + r.nextInt(10000));
				}
				counter ++;
				total++;
				signatures.setProperty("signatories", signatories );
				signatures.setProperty("total", total);
				if(total>=5000) {
					// ajout entity signatures dans la liste à insérer dans le datastore	
					signaturesList.add(signatures);
					response.getWriter().print("<li> creation of signatures:" + signatures.getKey() + "<br>");
					lastIndexOfSignatures = "petition_"+petitionNumber+"_start-"+counter;
					signatures = new Entity("Signatures",lastIndexOfSignatures,pet.getKey()); 
					pet.setProperty("signaturesIndex", lastIndexOfSignatures);
					signatories = new HashSet<String>();
					total=0;
					signatures.setProperty("total",total);
				}
			}
			pet.setProperty("counter", counter);
			petitionList.add(pet);
			signaturesList.add(signatures);
			response.getWriter().print("<li> creation of signatures:" + signatures.getKey() + "<br>");
		}
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		//Ajout des petitions
		datastore.put(petitionList);
		// Ajout des signatures
		datastore.put(signaturesList);
		response.getWriter().print("<li> fin de creation de "+ numberOfPetitions+" petitions");
	}
}