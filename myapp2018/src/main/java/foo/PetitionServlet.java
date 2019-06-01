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

@WebServlet(name = "PetitionServlet", urlPatterns = { "/petitions" })
public class PetitionServlet extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");

		response.getWriter().print("Populating....<br>");

		Random r = new Random();

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Map<String, Entity> users = new HashMap<String, Entity>();
		// Create Petitions 
		for (int i = 0; i < 200; i++) {			
			int randomCreator = r.nextInt(100);
			String creator = "u"+randomCreator+"@gmail.com";
			// création du user s'il n'existe pas 
			Entity user ;
			if(!users.containsKey(creator)) {
				user = new Entity("User", creator);
				user.setProperty("firstname", "firstname" + randomCreator);
				user.setProperty("lastname", "lastname" + randomCreator);
				ArrayList<String> myPetitions = new ArrayList<String>();
				myPetitions.add("pet"+i);
				user.setProperty("petitions",myPetitions);
				ArrayList<String> signatures = new ArrayList<String>();
				user.setProperty("signatures",signatures);

			}else{
				// ajout d'une petition dans la liste du user s'il existe déjà
				user = users.get(creator);
				@SuppressWarnings("unchecked") // Cast can't verify generic type.
				// TODO
				ArrayList<String> petitions = (ArrayList<String>) user.getProperty("petitions");
				petitions.add("pet"+i);
				user.setProperty("petitions",petitions);				
			}
			
			Entity pet = new Entity("Petition", "pet" + i,user.getKey()); // le créateur est le parent User
			pet.setProperty("title", "title" + i);
			pet.setProperty("description", "description" + i);
			users.put(creator,user);
			
			// ajout des signataires
			ArrayList<String> signatories = new ArrayList<String>();
			Integer counter=0;
			for (int j=0;j<r.nextInt(100);j++) {
				 String signatory = "u"+r.nextInt(100)+"@gmail.com" ;
				 if(!signatories.contains(signatory)) { // une seule signature par personne
					 signatories.add(signatory);
					 counter++;
				 }
				Entity indexSignatories=new Entity("SignatoriesIndex","signIndex"+i, pet.getKey());
				indexSignatories.setProperty("counter", counter); 
				 
				 //creation du user s'il n'existe pas .
				Key signatoryKey = KeyFactory.createKey("User", signatory);
				if(!users.containsKey(signatory)) {
					user = new Entity("User",signatory);
					user.setProperty("firstname", "firstname" + randomCreator);
					user.setProperty("lastname", "lastname" + randomCreator);
					ArrayList<String> petitions = new ArrayList<String>();
					user.setProperty("petitions",petitions);
					ArrayList<String> signatures = new ArrayList<String>();
					signatures.add("pet"+i);
					user.setProperty("signatures",signatures);
//						datastore.put(user);
					users.put(signatory,user);
				}else{
					// ajout d'une petition dans la liste du user s'il existe déjà
//							Entity user = datastore.get(userKey);
					 user = users.get(signatory);
					@SuppressWarnings("unchecked") // Cast can't verify generic type.
						ArrayList<String> signatures = (ArrayList<String>) user.getProperty("signatures");
					signatures.add("pet"+i);
					user.setProperty("signatures",signatures);				
					users.put(signatory,user);
				}
			}
			pet.setProperty("signatories", signatories);
			pet.setProperty("counter", counter);
			datastore.put(pet);
			
		// TODO  index sur le nombre de signature pour ressortir rapidement le top 100
			Entity index=new Entity("PetitionIndex","petIndex"+i, pet.getKey());
			index.setProperty("counter", counter);
			datastore.put(index);
			response.getWriter().print("<li> created petitions:" + pet.getKey() + " by "+pet.getParent()+"<br> added signatories ("+counter+")" + signatories + "<br>");
		}
		// ajout des users dans le datastore
		datastore.put(users.values());
		for (Entity u : users.values()) {
			response.getWriter().print("<li> created users:" + u.getProperty("firstname") + ",added petitions :" + u.getProperty("petitions") + "added signatures :"+ u.getProperty("signatures") +"<br>");
		}
	}
}