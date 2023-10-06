package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"net/http"
)

type AlbumInfo struct {
	Title  string `json:"title"`
	Artist string `json:"artist"`
	Year   string `json:"year"`
}

var albumData = map[int]AlbumInfo{
	1:  {Artist: "Taylor Swift", Title: "1989", Year: "2014"},
	2:  {Artist: "Joni Mitchell", Title: "Blue", Year: "1971"},
	3:  {Artist: "Bob Dylan", Title: "Nashville Skyline", Year: "1969"},
	4:  {Artist: "Black Sabbath", Title: "Master of Reality", Year: "1971"},
	5:  {Artist: "Pink Floyd", Title: "Wish You Were Here", Year: "1975"},
	6:  {Artist: "Kanye West", Title: "My Beautiful Dark Twisted Fantasy", Year: "2010"},
	7:  {Artist: "Fleetwood Mac", Title: "Tango in the Night", Year: "1987"},
	8:  {Artist: "Chance The Rapper", Title: "Acid Rap", Year: "2013"},
	9:  {Artist: "Dire Straits", Title: "Brothers in Arms", Year: "1985"},
	10: {Artist: "Jim Croce", Title: "You Don’t Mess Around With Jim", Year: "1972"},
}

func getAlbums(c *gin.Context) {
	c.IndentedJSON(http.StatusOK, albumData)
}

func getAlbumByID(c *gin.Context) {
	id := c.Param("id")
	albumID := atoi(id)

	album, ok := albumData[albumID]
	if ok {
		c.IndentedJSON(http.StatusOK, album)
		return
	}
	c.IndentedJSON(http.StatusNotFound, gin.H{"message": "album not found"})
}

func atoi(s string) int {
	var res int
	_, err := fmt.Sscanf(s, "%d", &res)
	if err != nil {
		return 0
	}
	return res
}

func postAlbum(c *gin.Context) {
	var newAlbum AlbumInfo

	profileJSON  := c.Request.PostFormValue("profile")
	imageFile, _ , imageErr := c.Request.FormFile("image")

	if profileJSON == "" {
		c.IndentedJSON(http.StatusBadRequest, gin.H{"message": "invalid form"})
		return
	}

	imageSize := 0

	if imageErr != nil && imageFile != nil {
		defer imageFile.Close()
		buf := make([]byte, 1024)
		for {
			n, err := imageFile.Read(buf)
			if err != nil {
				break
			}
			imageSize += n
		}
	}

	albumID := len(albumData) + 1
	albumData[albumID] = newAlbum
	c.IndentedJSON(http.StatusCreated, gin.H{"message": "album created successfully", "album_id": albumID, "image_size": imageSize})

}

func main() {
	router := gin.Default()
	router.GET("/albums", getAlbums)
	router.GET("/albums/:id", getAlbumByID)
	router.POST("/albums", postAlbum)

	router.Run(":8080")
}
