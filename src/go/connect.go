package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
)

func getIntent() string {
	response, err := http.Get("http://127.0.0.1:8000/get")

	if err != nil {
		panic(err)
	}
	defer response.Body.Close() // defer causes deferred functions to be called at the end in lifo order, so cool

	body, err := io.ReadAll(response.Body)
	if err != nil {
		fmt.Println("Error reading response:", err)
		return ""
	}

	var data map[string]string
	err = json.Unmarshal(body, &data)
	if err != nil {
		fmt.Println("Error reading JSON: ", err)
		return ""
	}

	intent, exists := data["intent"]
	if exists {
		fmt.Println(intent)
	}

	return intent
}
