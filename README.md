# tinypet
Valentin Gillet - Arnaud Loiseau - Aldvine Blanchard

# lien vers l'applicatoion
https://cloud-miage.appspot.com

/populate -> servlet pour peupler la base (génère aléatoirement 1 à 7 petitions avec ses signatures (1 à 6000 signatures)

Sur l'application une authentification est nécéssaire pour ajouter une pétition et signer un petition. 
L'adresse mail du compte google sert d'utilisateur
Pour faire fonctionner l'application remplacez la constante "clientIds" du fichier petitionEndpoint par la votre pour autoriser la connexion via un compte google
ID clients OAuth 2.0: 
https://console.cloud.google.com/apis/credentials

Modifier l'URL du serveur cible dans le fichier main.js 


# Install

* Add gcloud in your path:
* * export PATH=$PATH:~/.cache/google-cloud-tools-java/managed-cloud-sdk/LATEST/google-cloud-sdk/bin/
* * Or cp ~molli-p/.bashrc ~ and source ~/.bashrc
* gcloud config set account yourname
* gcloud auth login
* gcloud config set project yourproject
* mvn appengine:deploy
* gcloud app browse

# use
https://yourapp.appstpot.com/_ah/api/explorer



