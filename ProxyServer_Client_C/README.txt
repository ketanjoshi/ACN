/*
 *	ACN Homework Assignment 1 
 *	Title: HTTP GET Request Proxy Server
 *	Author: Ketan Joshi
 */

Important Instructions & Notes:
1) Compile the server using: gcc proxyServer.c -lpthread -o server
2) Compile the client using: gcc proxyClient.c -o client
3) When running the program provide the proxy server IP as an argument to the client. No arguments are required for proxy server.
e.g.-	./server and ./client 127.0.0.1
4) The server asks the page request to the client in a specific format. Please enter the command in that specific pattern. The server needs the same pattern to parse it so that it can fetch the requested page.
e.g.-	host: www.utdallas.edu port: 80 file: /~ksarac/index.html
	host: www.amazon.com port: 80 file: /index.html
	host: www.google.com port: 80 file: /index.html
5) The server will create the output files with name "fileop_srv<client_connection_number>.html" corresponding to each client.
So the client which gets connected first, the output file for it will be fileop_srv1.html.
6) Client creates the output file by fileop.html. So if you are connecting multiple clients then launch the exe from different folders, otherwise the client output files will get overwritten.
