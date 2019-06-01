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
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.repackaged.com.google.datastore.v1.CompositeFilter;
import com.google.appengine.repackaged.com.google.datastore.v1.Projection;
import com.google.appengine.repackaged.com.google.datastore.v1.PropertyFilter;

@WebServlet(name = "PetitionQuery", urlPatterns = { "/petitionQuery" })
public class PetitionQuery extends HttpServlet {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");

		String user = request.getParameter("user");
		if (user == null) {
			user = "u50@gmail.com";
		}
		
		Integer top = 100;
		// requete du top 100 des petition les plus signés
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Petition").addSort("counter", SortDirection.DESCENDING);
//		q.setKeysOnly();
		PreparedQuery pq = datastore.prepare(q);
		
		List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(top));
		List<Key> pk = new ArrayList<Key>();
		
//		for (Entity res : results) {
//			pk.add(res.getParent());
//		}
//		Map<Key, Entity> hm = new HashMap<Key, Entity>();
//		hm = datastore.get(pk);
//		
		response.getWriter().print("<li> result: le top" + results.size() + " des pétitions les plus signées<br>");
		for (Entity entity : results) {
			response.getWriter().print("<li>" + entity.getProperty("title") + "," + entity.getProperty("description")
					+ "," + entity.getProperty("counter"));
		}

//		q = new Query("Petition")
//				.setFilter(CompositeFilterOperator.and(
//						new FilterPredicate("signatories", FilterOperator.EQUAL, "u2@gmail.com"),
//					//	new FilterPredicate("signatories", FilterOperator.EQUAL, "u50"),
//						new FilterPredicate("title", FilterOperator.GREATER_THAN_OR_EQUAL, 500))); //and >= ??
//		
//		pq = datastore.prepare(q);
//		result = pq.asList(FetchOptions.Builder.withDefaults());
//
//		response.getWriter().print("<li> result:" + result.size() + "<br>");
//		for (Entity entity : result) {
//			response.getWriter().print("<li>" + entity.getProperty("title")+","+entity.getProperty("signatories"));
//		}


//		long t1=System.currentTimeMillis();
//		q = new Query("Friend");
//		pq = datastore.prepare(q);
//		result = pq.asList(FetchOptions.Builder.withDefaults());
//
//		response.getWriter().print("<li> result:" + result.size() + "<br>");
//		for (Entity entity : result) {
//			response.getWriter().print("<li>" + entity.getProperty("firstName"));
//		}
//		long t2=System.currentTimeMillis();
//
//		q = new Query("Friend");
//		q.addProjection(new PropertyProjection("firstName",String.class));
//		pq = datastore.prepare(q);
//		result = pq.asList(FetchOptions.Builder.withDefaults());
//
//		response.getWriter().print("<li> result:" + result.size() + "<br>");
//		for (Entity entity : result) {
//			response.getWriter().print("<li>" + entity.getProperty("firstName"));
//		}
//		long t3=System.currentTimeMillis();
//		response.getWriter().print("q1:"+(t2-t1)+","+"q2:"+(t3-t2));
		
		
	}
}
