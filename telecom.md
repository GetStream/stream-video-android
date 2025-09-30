# New classes

1. ServiceLauncher
2. TelecomVoipService
3. CallRejectionHandler
4. TelecomServiceLauncher


## Class name: 
- ServiceLauncher

### Purpose:
- Launches Telecom and CallingService

### Methods:
- showIncomingCall
- removeIncomingCall
- showOnGoingCall
- showOutgoingCall
- stopService

## Class name: 
- TelecomVoipService

### Purpose:
- Connection Service which creates successful or failed connection

## Class name: 
- CallRejectionHandler

### Purpose:
- Handles the call rejection broadcast event

### Methods:
- reject

## Class name:
- TelecomServiceLauncher

### Purpose:
- Launched ConnectionService

### Methods:
- addIncomingCallToTelecom
- addOutgoingCallToTelecom
- addOnGoingCall







