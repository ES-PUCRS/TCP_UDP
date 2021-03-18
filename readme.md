# TCP UDP
server/client app used to test network protocols

## How to run
inside each folder (UDP/TCP) there is a run.bat file which will open two terminal with Client and Server connections.

## Difference between TCP and UDP connections
TCP will keep a connection alive until it gets a response, while this response does not come from the destiny, the client are supposed to send another data package. Seeing that the server would never send a response, the client can provide limited forwards messages before give up and release the connection to another possible call. Therefore the server need to be able to reply the message or the client will overflow the server port.

UDP does not care about response and just send the package and we 'hope' that the server are able to catch it. In spite of the absence of reply, it is a bit faster transport protocol.


The principal development difference is that with the UDP we only need to send the message whenever is there a server to catch. On other hand, the TCP connection needs to send the package and get instantly ready to read the response, also, due to prevent that the connection get never closed with the message never received, we still need to validade how many packages we have sent without answare and how long are we spending on this single package transfer foreseing a new send request on the queue.
