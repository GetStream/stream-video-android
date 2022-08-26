package video_events

import (
	"github.com/GetStream/video/pkg/validator"
)

func init() {
	validator.Register[AuthPayload](map[string]string{
		"User":   "required",
		"ApiKey": "required,min=1,max=16",
		"Token":  "required,max=1000,jwt",
	})
}
