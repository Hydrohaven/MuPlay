package main

import (
	"fmt"
)

func main() {
	intent := getIntent()
	fmt.Println(intent)

	userAuth()
}
