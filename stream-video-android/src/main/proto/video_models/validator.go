package video_models

import "github.com/GetStream/video/internal/validator"

func init() {
	validator.Register[UserRequest](map[string]string{
		"Id":              "required,max=1",
		"Name":            "max=255",
		"ProfileImageUrl": "max=512",
		"Teams":           "max=25",
	})
}
