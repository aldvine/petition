# tinypet
Valentin Gillet - Arnaud Loiseau - Aldvine Blanchard

# Les liens

Application
https://cloud-miage.appspot.com

Explorer API
https://apis-explorer.appspot.com/apis-explorer/?base=https%3A%2F%2Fcloud-miage.appspot.com%2F_ah%2Fapi&root=https%3A%2F%2Fcloud-miage.appspot.com%2F_ah%2Fapi#p/myApi/v1/

Servlet pour peupler la base (génère aléatoirement 1 à 7 petitions avec ses signatures (1 à 6000 signatures)
https://cloud-miage.appspot.com/populate

# Schema
![Alt text](/kind_Petition.PNG?raw=true "Kind Petition")
Sur la capture d'écran, nous voyons en rouge une pétition ayant atteint plus de 5000 signature.
Le champ signaturesIndex permet de stocker la liste des signataires qui n'est pas remplie. 

![Alt text](/kind_Signatures.png?raw=true "Kind Petition")
En rouge, on remarque les deux listes de signataires qui sont de la même pétition, lorsqu'une liste est complète (5000 dans notre cas),  on crée une nouvelle liste et le champ signaturesIndex de Petition se met à jour. 

# Mémo
Sur l'application une authentification est nécéssaire pour ajouter une pétition et signer une petition. 
L'adresse mail du compte google sert d'utilisateur

Pour faire fonctionner l'application sur remplacez la constante "clientIds" du fichier petitionEndpoint.java et le client-id de la fonction initGoogleAuth par celui correspondant pour autoriser la connexion via un compte google

ID clients OAuth 2.0: 
https://console.cloud.google.com/apis/credentials

Modifier l'URL du serveur cible dans le fichier main.js 




