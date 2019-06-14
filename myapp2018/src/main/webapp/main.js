//var root= "http://localhost:8080/_ah/api/myApi/v1"  
var root= "https://cloud-miage.appspot.com/_ah/api/myApi/v1" 

var gapi = gapi || {};
function loadAuthClient () {
	  gapi.load('auth2', initGoogleAuth);
	}
function initGoogleAuth (clientId = "122644336388-92j45odst8ekhc91lhtj49rv4ipr8q61.apps.googleusercontent.com") {
  gapi.auth2.init({
    client_id: clientId,
    scope: 'https://www.googleapis.com/auth/userinfo.email'
  }).then(() => {
    console.log("init");
    if(gapi.auth2.getAuthInstance().isSignedIn.get()){
		Menu.connected=true;
	}else{
		Menu.connected=false;
	}
//    console.log(Menu.connected);
	m.redraw();
  }).catch(err => {
    console.log(err);
  });
}


var notifications =[];
var notificationNumber = 0;
function addNotification(type, message){
	// si notification valide
	if(type && message){
		notificationNumber++;
		notifications.push({id:notificationNumber,type:type,message:message});
	}
}

var main = {
	view:function(){
		return m("main.has-background-light",	
		[
			m(Menu),
			m(Petitions)
		])
	}
}
function signIn(){
	gapi.auth2.getAuthInstance().signIn().then(function() {
		var user = gapi.auth2.getAuthInstance().currentUser.get();
	    var idToken = user.getAuthResponse().id_token;
	    let message="Vous êtes connecté en tant que "+user.getBasicProfile().getName()+" ("+user.getBasicProfile().getEmail()+")";
		addNotification("success",message);
		isSignedIn();
		m.redraw();
	  }).catch(err => {
	    console.log(err);
	  });
}
function signOut(){
	 gapi.auth2.getAuthInstance().signOut().then(() => {
		 let message="Vous êtes déconnecté(e).";
    		addNotification("warning",message);
    		isSignedIn();
    		m.redraw();
		  }).catch(err => {
		    console.log(err);
		  });
}

function isSignedIn(){
	let connected = gapi.auth2.getAuthInstance().isSignedIn.get();
	if(gapi.auth2.getAuthInstance().isSignedIn.get()){
		Menu.connected=connected;
	}else{
		Menu.connected=connected;
	}
	return connected;
}
var Menu = {
		connected:false,
		view:function(){
			return m("nav.navbar is-info",{role:"navigation","aria-label":"main navigation"},[
				
	       		m("div.navbar-brand",[
	       			m("a.navbar-item",[
	       				m("span","TinyPet")
	    			])
	    			,m("a.navbar-item",{target:"_blank",href:"https://github.com/aldvine/petition"},[
	       				m("a.button","Vers Github")
		    		]),
		    		m("div.buttons",[
						m("a.button is-success sign-in-btn",{style:"display:"+(Menu.connected ? "none":"inline-flex"),onclick:function(){
							signIn();
						}},"Connexion (Google)"),
						m("a.button is-danger sign-out-btn",{style:"visibility:"+(Menu.connected ? "inline-flex":"none"),onclick:function(){
							signOut();
						}},"Déconnexion")
					])
				]),
			]);
		}
}

var Petitions = {
	list:[],
	activeView:"all",
	view: function () {
		var petitionsView = m("div.container",{oninit:function(){
			 m.request({
			        method: "GET",
			        url: root+"/petitions",
			    })
			    .then(function(data) {
			    	Petitions.list = data.items;
//			  		console.log(Petitions.list); 
			    })
		},oncreate:function(){
			// connexion google

		} }, [
			m("div",[
			
				m("a.button is-primary is-link is-rounded", {class:(Petitions.activeView== "FormPetition" ? "":"is-outlined"), 
					onclick: function () {
						Petitions.activeView="FormPetition"
					}
				}, "Ajouter une pétition"),
				m("a.button is-primary is-link is-rounded", {class:(Petitions.activeView== "all" ? "":"is-outlined"), 
					onclick: function () {
						Petitions.activeView="all"
						Petitions.list=[]
						 m.request({
						        method: "GET",
						        url: root+"/petitions",
						    })
						    .then(function(data) {
						    	Petitions.list = data.items;
//						    	console.log(Petitions.list); 
						    })
					}
				}, "Toutes les pétitions"),
				m("a.button is-link is-rounded ", { class:(Petitions.activeView== "top" ? "":"is-outlined"), 
					onclick: function () {
						Petitions.list=[]
						 Petitions.activeView="top"
						 m.request({
						        method: "GET",
						        url: root+"/petition/top",
						    })
						    .then(function(data) {
						    	Petitions.list = data.items;
						    })
					}
				}, "Top 100"),
				m("a.button is-link is-rounded ", { class:(Petitions.activeView== "SignedByUser" ? "":"is-outlined"), 
						onclick: function () {
							 Petitions.activeView="SignedByUser"
							 Petitions.list = [];
						}
					}, "Signées par utilisateur"),
				m("a.button is-link is-rounded ", { class:(Petitions.activeView== "MyPetitions" ? "":"is-outlined"), 
						onclick: function () {
							if(isSignedIn()){
								Petitions.activeView="MyPetitions"
								Petitions.list = [];
								var user = gapi.auth2.getAuthInstance().currentUser.get();
								m.request({
							        method: "GET",
							        url: root+"/user/"+encodeURIComponent(user.getBasicProfile().getEmail())+"/signatures"
							    })
							    .then(function(data) {
								    Petitions.list = data.items;
							    });	
							}else{
							 	signIn();
							}
						}
					}, "Mes pétitions"),
				m("div.section",
						[
							Petitions.activeView== "top" ? petitionTop(Petitions.list): "",
							Petitions.activeView== "all" ? petitionAll(Petitions.list): "",
							Petitions.activeView== "SignedByUser" ? m(SignedByUser): "",
							Petitions.activeView== "MyPetitions" ? m(MyPetitions): "",
							Petitions.activeView== "FormPetition" ? m(FormPetition): ""
					])
			])
			,	m("div",{style:"position:fixed; bottom:0;right:0;padding:20px;"},notificationList(notifications))])
			
		return petitionsView;
	}
}

var SignedByUser = {
		username:"",
		view:function(){
			return m("div",[
				m("h2.subtitle","Recherche des pétitions signées par un utilisateur"),
				m("label.label","email"),
				m("input.input [type=text]",{oninput: function (e) { 
					SignedByUser.username = e.target.value
					},
					value: SignedByUser.username},
					"email"),
				m("div",[
					m("a.button is-success [type=text]",{
						onclick:function(){
							if(SignedByUser.username !=""){
								m.request({
							        method: "GET",
							        url: root+"/user/"+encodeURIComponent(SignedByUser.username)+"/signatures"
							    })
							    .then(function(data) {
								    Petitions.list = data.items;
							    });	
							}
							 
						}
					},"Rechercher"),
				]),
				
				m("div",[
					Petitions.list.length >0 ?petitionList(Petitions.list) : "Aucune pétition à afficher"
				])
			]);
		}
};

var MyPetitions = {
		username:"",
		view:function(){
			return m("div",[
				m("h2.subtitle","Liste de mes pétitions"),			
				m("div",[
					Petitions.list.length >0 ?petitionList(Petitions.list) : "Aucune pétition à afficher"
				])
			]);
		}
};


var duration = 5000; // 5 secondes entre chaque fermeture de notification automatique
function notificationList(notifs){

	return notifs.map(function(n,index) {
		duration+=5000;
		setTimeout(function(){
			notifications.splice(index,1);
			m.redraw(); duration -=5000;},duration);
        return m("div.notification",{class: "is-"+n.type}, [
	        	m("button.delete",{onclick:function(){
	        		notifications.splice(index,1);
	        	}}),
	        	m("span",n.message)
	        ])
	});
}

function petitionAll(){
	return m("div", [
		m("a.button is-primary",{
			onclick:function(){
				console.log(Petitions.list);
				var lastPetitionName = Petitions.list[Petitions.list.length - 1].key.name;
				 m.request({
				        method: "GET",
				        url: root+"/petitions",
				    })
				    .then(function(data) {
					    	Petitions.list = data.items;
				    })
			}
		}, "Rafraichir la liste"),
		m("a.button is-info",{
			onclick:function(){
				console.log(Petitions.list);
				var lastPetitionName = Petitions.list[Petitions.list.length - 1].key.name;
				 m.request({
				        method: "GET",
				        url: root+"/petitions?next="+encodeURIComponent(lastPetitionName),
				    })
				    .then(function(data) {
				    	if(data.items.length>0){
					    	Petitions.list = data.items;
				    	}
				    })
			}
		}, "Suivant"),
		petitionList(Petitions.list),
		
    ])
}

function petitionList(petitions){
	return m("div",
			m("table.table  is-fullwidth",[
				m("thead",[
			       	m("tr", [
					m("th","Titre"),
					m("th","Signatures"),
					m("th","Description"),
					m("th","Action"),
			        ])
				]),
				m("tbody",[
					petitions.map(function(p,index) {
				      return   m("tr", [
							m("th",p.properties.title),
							m("td",p.properties.counter),
							m("td",p.properties.description),
							m("td",buttonSignature(p)),
				        ])
				    })
				])
				
			])
		)
}
function petitionTop(petitions){
	return m("div",
			m("table.table  is-fullwidth",[
				m("thead",[
			       	m("tr", [
			       	m("th","TOP"),
					m("th","Titre"),
					m("th","Signatures"),
					m("th","Description"),
					m("th","Action"),
			        ])
				]),
				m("tbody",[
					petitions.map(function(p,index) {
				      return   m("tr", [
				    	  	m("th"," #"+(index+1)),
							m("th",p.properties.title),
							m("td",p.properties.counter),
							m("td",p.properties.description),
							m("td",buttonSignature(p)),
				        ])
				    })
				])
				
			])
		)
}
function buttonSignature (p){
	var hideButton=false;	
	 return m("a.button is-info",{style:"display:"+(hideButton ? "none":"inline-flex"),	id:p.properties.title+"_signer",onclick:function(){
		 	if(isSignedIn()){
				var user = gapi.auth2.getAuthInstance().currentUser.get();
			    var idToken = user.getAuthResponse().id_token;
				 m.request({
			        method: "POST",
			        url: root+"/signature?access_token=" + encodeURIComponent(idToken),
			        data:{signatory:"test" ,title:p.properties.title}
			    })
			    .then(function(data) {
			    	type = data.code.substring(0, 2) =="20" ? "success" : "danger";
			    	if(data.code=="200" || data.code =="403"){
			    		// TODO cacher le bouton
			    	}
			    	addNotification(type,data.message);
			    });
		 	}else{
		 		signIn();
		 	}
		}},"Signer");

}

function getPetitions(){
	return  m.request({
        method: "GET",
        url: root+"/petitions",
    })
    .then(function(data) {
    	Petitions.list = data.items;
    })
}


var FormPetition = {
	title:'',
	description:'',
	view: function () {
		return m("form", {
			onsubmit: function (e) {
				e.preventDefault()
				if(FormPetition.title != "" && FormPetition.description!=""){
					if(isSignedIn()){
						var user = gapi.auth2.getAuthInstance().currentUser.get();
					    var idToken = user.getAuthResponse().id_token;
						m.request({
						    method: "POST",
						    url: root+"/petition?access_token=" + encodeURIComponent(idToken),
						    data:{title:FormPetition.title,description:FormPetition.description}
						})
						.then(function(data) {
							if(data!= null){
								Petitions.list.unshift(data);
								addNotification("success","Petition ajoutée !");
							}
						},function(error){
							addNotification("danger","Une erreur est survenue");
						})
					}else{
						signIn();
					}
					
				}
			}
		}, [
				m("h1.subtitle", "Création d'une pétition"),
				m("label.label", "titre"),
	 			m("input.input[type=text][placeholder=titre][maxlength=150]", {
					oninput: function (e) { 
						FormPetition.title = e.target.value
						},
					value: FormPetition.title
				}), 
				m("label.label", "Description"),
				m("textarea.textarea[placeholder=description][maxlength=2000]", {
					oninput: function (e) {  FormPetition.description = e.target.value },
					value:  FormPetition.description
				}),
				m("button.button[type=submit] .button is-rounded is-success", "Ajouter"),
			])
	}
}

m.route(document.body, "/",{
		"/":main})

