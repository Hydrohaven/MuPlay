package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"

	spotifyauth "github.com/zmb3/spotify/v2/auth"

	"github.com/joho/godotenv"
	"github.com/zmb3/spotify/v2"
)

const redirectURL string = "http://localhost:8080/callback"

var (
	auth  *spotifyauth.Authenticator
	ch    = make(chan *spotify.Client)
	state = "muplay-auth" // what?
)

// Initializes global auth, ch, and state variables using .env ID's and redirectURL
func initialize() {
	err := godotenv.Load()
	if err != nil {
		log.Fatal("Error loading .env file")
	}

	requiredEnvVars := []string{"SPOTIFY_CLIENT_ID", "SPOTIFY_CLIENT_SECRET"}
	for _, envVar := range requiredEnvVars {
		if os.Getenv(envVar) == "" {
			log.Fatalf("Missing required environment variable: %s", envVar)
		} else {
			fmt.Println(envVar, os.Getenv(envVar))
		}
	}

	auth = spotifyauth.New(
		spotifyauth.WithRedirectURL(redirectURL),
		spotifyauth.WithScopes(spotifyauth.ScopeUserReadPrivate),
		spotifyauth.WithClientID(os.Getenv("SPOTIFY_CLIENT_ID")),
		spotifyauth.WithClientSecret(os.Getenv("SPOTIFY_CLIENT_SECRET")),
	)
}

// Authenticates a user using global auth and ch variables
func userAuth() {
	initialize()

	http.HandleFunc("/callback", completeAuth)
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		log.Println("Got request for:", r.URL.String())
	})

	go func() {
		err := http.ListenAndServe(":8080", nil)
		if err != nil {
			log.Fatal(err)
		}
	}()

	url := auth.AuthURL(state)
	fmt.Println("Please login to Spotify by visiting the following page:", url)

	// wait for auth to complete
	client := <-ch

	// use the client to make calls that require authorization
	user, err := client.CurrentUser(context.Background())
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("You are logged in as:", user.DisplayName)
}

// Not sure what this does yet
func completeAuth(w http.ResponseWriter, r *http.Request) {
	token, err := auth.Token(r.Context(), state, r)
	if err != nil {
		http.Error(w, "Couldn't get token", http.StatusForbidden)
		log.Fatal(err)
	}

	if st := r.FormValue("state"); st != state {
		http.NotFound(w, r)
		log.Fatalf("State mismatch: %s != %s\n", st, state)
	}

	client := spotify.New(auth.Client(r.Context(), token))
	fmt.Fprintf(w, `
		<body style="background: #1f1f1f;">
		<div style="display: flex; min-height: 90vh; text-align: center; justify-content: center; align-items: center; font-size: 40px; font-family: Lucida Sans Unicode; color: #1ed760;">
			Login Completed! <br>
			Return to µPlay!
		</div>
		</body>
	`)
	ch <- client

}
