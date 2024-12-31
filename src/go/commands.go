package main

import (
	"context"
	"encoding/json"
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
	auth   *spotifyauth.Authenticator
	ch     = make(chan *spotify.Client)
	state  = "muplay-auth" // for cross-site request forgery (csrf) protection?
	client *spotify.Client
)

func startSpotify() {
	initialize()  // Initialize variables and environment
	userAuth()    // Authenticate the user
	startServer() // Start the Go HTTP server and handle Spotify commands
}

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
		spotifyauth.WithScopes(spotifyauth.ScopeUserModifyPlaybackState),
		spotifyauth.WithClientID(os.Getenv("SPOTIFY_CLIENT_ID")),
		spotifyauth.WithClientSecret(os.Getenv("SPOTIFY_CLIENT_SECRET")),
	)

	fmt.Println()
}

// Starts the HTTP server to listen for post requests to execute Spotify commands
func startServer() {
	http.HandleFunc("/spotify", modifyPlayback) // endpoint for intents
	log.Println("Server is running on http://localhost:8081")

	err := http.ListenAndServe(":8081", nil)
	if err != nil {
		log.Fatal("Error startign server", err)
	}
}

func modifyPlayback(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Invalid request method", http.StatusMethodNotAllowed)
		return
	}

	var payload struct {
		Intent string `json: "intent"`
	}

	if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
		http.Error(w, "Invalid request payload", http.StatusBadRequest)
		log.Println("Error decoding request body", err)
		return
	}

	log.Println("Received intent:", payload.Intent)

	// client := <-ch
	ctx := context.Background()

	switch payload.Intent {
	case "Play the song":
		executeCommand(client.Play, ctx, w)
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Playback Started"))
	case "Pause the song":
		executeCommand(client.Pause, ctx, w)
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Playback Paused"))
	case "Skip the song":
		executeCommand(client.Next, ctx, w)
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Skipped song"))
	case "Play the last song":
		executeCommand(client.Previous, ctx, w)
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("Previous song"))
	case "Shut down":
		os.Exit(1)
	}

}

func executeCommand(f func(context.Context) error, ctx context.Context, w http.ResponseWriter) {
	if err := f(ctx); err != nil {
		log.Println("Error with playback:", err)
		http.Error(w, "Failed to modify playback", http.StatusInternalServerError)
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("Playback Started"))
}

// Authenticates a user using global auth and ch variables
func userAuth() {
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

	client = <-ch
}

// Verifies authentication request and stores new client in the chanel ch
func completeAuth(w http.ResponseWriter, r *http.Request) {
	// Exchanges auth code from callback request for an access token
	fmt.Println("Attempting to complete authentication")
	token, err := auth.Token(r.Context(), state, r)
	if err != nil {
		http.Error(w, "Couldn't get token", http.StatusForbidden)
		log.Fatal(err)
	}

	// Verifies the state (request is coming from muplay)
	if st := r.FormValue("state"); st != state {
		http.NotFound(w, r)
		log.Fatalf("State mismatch: %s != %s\n", st, state)
	}

	// Authorizes new client after getting through token retrieval and state verification
	newClient := spotify.New(auth.Client(r.Context(), token))
	fmt.Fprintf(w, `
		<body style="background: #1f1f1f;">
		<div style="display: flex; min-height: 90vh; text-align: center; justify-content: center; align-items: center; font-size: 40px; font-family: Lucida Sans Unicode; color: #1ed760;">
			Login Completed! <br>
			Return to µPlay!
		</div>
		</body>
	`)
	ch <- newClient
}
