/*
 *	ACN Homework Assignment 1 
 *	Title: HTTP GET Request Proxy Client-Server Model
 *	Author: Ketan Joshi
 *	Client Code
 */


#include <netinet/in.h>
#include <stdio.h>  
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>

void str_cli(int);

int main(int argc, char* argv[])
{
	int sockfd;
	struct sockaddr_in servaddr;

	// Creating a socket
	if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0)
	{
		printf("\n%s: Error in socket", argv[0]);
		exit(0);
	}

	// Configuring proxy server address structure
	bzero(&servaddr, sizeof(servaddr));
	servaddr.sin_family = AF_INET;
	servaddr.sin_port = htons(33000);

	if ((inet_pton(AF_INET, argv[1], &servaddr.sin_addr)) <= 0)
	{
		printf("\n%s: Error in inet_pton", argv[0]); 
		exit(0);
	}
	
	fprintf(stderr, "\nConnecting... %d %d", servaddr.sin_port, servaddr.sin_addr.s_addr);

	// Connecting to the proxy server
	if ((connect(sockfd, (struct sockaddr *) &servaddr, sizeof(servaddr))) < 0)
	{
		fprintf(stderr, "\n%s: Error in connect : %d", argv[0], errno);
		exit(0);
	}
	fprintf(stderr, "\nSuccessfully connected");

	str_cli(sockfd);
	return 1;
}

void str_cli (int sockfd)
{
	char sendline[1024], recvline[1024];
	int n;
	FILE* fp;
	char htmlfile[100];
	char html[6];

	// Reading incoming data from proxy server
	bzero(recvline, 1024);
	read(sockfd, recvline, 1024);
	fprintf(stderr, "\n%s", recvline);
	
	// Generating filename
	strcpy(html, ".html");
	strcpy(htmlfile, "fileop");
	strcat(htmlfile, html);

	// Reading the command from stdin
	if (fgets(sendline, 1024, stdin) == NULL)
	{
		fprintf(stderr, "\nError in fgets");
		exit(0);
	}

	// Sending command to proxy server
	if (write(sockfd, (const void*) sendline, strlen(sendline)) < 0)
	{
		fprintf(stderr, "\nError in write");
		exit(0);
	}

	// Get the requested page from the proxy server and display it on the terminal
	fp = fopen(htmlfile, "w+");
	while ((n = read(sockfd, recvline, 1024)) > 0)
	{
		recvline[n] = 0;
		fputs(recvline, stdout);
		fputs(recvline, fp);
		bzero(recvline, 1024);
	}
	fclose(fp);
	close(sockfd);
}
