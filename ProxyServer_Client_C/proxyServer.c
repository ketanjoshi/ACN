/*
 *	ACN Homework Assignment 1 
 *	Title: HTTP GET Request Proxy Client-Server Model
 *	Author: Ketan Joshi
 *	Server Code
 */


#include <netinet/in.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static void *ListenToClients(void *);
static void *SelectReadSockets(void *);
static void *ServeClient(void *);

#define SPACES "\r\n\r\n"
#define GET "GET "
#define HTTP_GET_PARAM "HTTP/1.1\r\nHost: "

struct addrinfo hints, *servinfo, *p;
struct timeval t;
int rv, workerfd[20], remotefd[20], finishfd[20], totalconn = 0;
fd_set readfds1, readfds2;
pthread_t tid[20], pid[20], listenthread, selectthread;
int listenfd, connfd;
struct sockaddr_in cliaddr, proxyservaddr;

int main(int argc, char **argv)
{
	int flags, sockfd, th=0, max_sd1, max_sd2, activity, i;
	
	for(i = 0; i < 10; i++)
		finishfd[i] = 0;

	t.tv_sec = 2;
	t.tv_usec = 0;

	// Creating a socket
	if ((listenfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
	printf("\n%s: Error in socket", argv[0]);
	exit(0);
	}

	// Configuring server address structure
	bzero(&proxyservaddr, sizeof(proxyservaddr));
	proxyservaddr.sin_family      = AF_INET;
	proxyservaddr.sin_addr.s_addr = htonl(INADDR_ANY); 
	proxyservaddr.sin_port        = htons(33000);

	// Binding our socket to the service port
	if (bind(listenfd, (struct sockaddr*) &proxyservaddr, sizeof(proxyservaddr)) < 0) {
	printf("\n%s: Error in bind", argv[0]);
	exit(0);
	}
	fprintf(stderr, "\nBefore thread launch");
	
	pthread_create(&listenthread, NULL, ListenToClients, (intptr_t) listenfd);
	pthread_create(&selectthread, NULL, SelectReadSockets, (intptr_t) listenfd);
	
	pthread_join(listenthread, NULL);
	pthread_join(selectthread, NULL);

}

static void * ListenToClients(void * arg)
{
	char sendline[1024];
	socklen_t clilen;
	int max_sd, activity;
	while(1)
	{
		FD_ZERO(&readfds1);
		if (listen(listenfd, 5) < 0)
		{
			printf("\nError in listen"); 
			exit(0);
		}

		clilen = sizeof(cliaddr);
		FD_SET(listenfd, &readfds1);
		max_sd = listenfd + 1;
		activity = select(max_sd , &readfds1 , NULL , NULL , &t);

		// Accept a new connection and return a new worker socket to handle the new client
		if(FD_ISSET(listenfd, &readfds1))
		{
			fprintf(stderr, "\nInside listen isset");
			if ((workerfd[totalconn] = accept(listenfd, (struct sockaddr*) &cliaddr, &clilen)) < 0)
			{
				fprintf(stderr, "\nError in accept");
				exit(0);
			}
			totalconn++;
			fprintf(stderr, "\nConnected socket : %d", workerfd[totalconn - 1]);
			bzero(sendline, 1024);
			strcpy(sendline, "Enters details is format \"host: www.abc.com port: 80 file: /example.html\":\n");
			write(workerfd[totalconn - 1], sendline, strlen(sendline));
		}	
	}
}

static void * SelectReadSockets(void * arg)
{
	int i, j = 0, k = 0, activity, max_sd2;
	while(1)
	{
		FD_ZERO(&readfds2);
		// Implementing select()
		// Adding the different client sockets to the set readfds
		for(i=0; i<totalconn; i++)
		{
			if(finishfd[i] == 0)
				FD_SET(workerfd[i], &readfds2);
		}
		// Highest descriptor number for the select function
		max_sd2 = workerfd[totalconn-1] + 1;

		// Waiting for an activity on one of the sockets , timeout is NULL , so wait indefinitely
		activity = select( max_sd2 , &readfds2 , NULL , NULL , &t);
		if (activity < 0)
		{		
			fprintf(stderr, "\nselect() error");
		}

		for(i=0; i<totalconn; i++)
		{
			if((finishfd[i] == 0) && (FD_ISSET(workerfd[i], &readfds2)))
			{
				fprintf(stderr,"\ni = %d, finished = %d", i, finishfd[i]);
				pthread_create(&pid[i], NULL, &ServeClient, (intptr_t) i);
				pthread_join(pid[i], NULL);
			}
		}
	}
}


static void * ServeClient(void *arg)
{
	int current, socketfd;

	char sendline[1024], recvline[1024], command[256], host[100];
	char socknum[2];
	char htmlfile[100];
	char html[6];
	int n, sockfd, flags, remoteservfd;
	FILE* fp;

	current = (int) arg;
	sockfd = workerfd[current];

	// Parsing the command and find out server part and page part
	bzero(recvline, 1024);
	read(sockfd, recvline, 1024);
	fprintf(stderr, "\nCommand received on socket %d : %s", sockfd, recvline);
	GetHttpRequest(recvline, &command, &host);
	fprintf(stderr, "\n%s", command);
	
	bzero(&hints, sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	
	if ((rv = getaddrinfo(host, "http", &hints, &servinfo)) != 0)
	{
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		exit(1);
	}
	p = servinfo;

	// Creating a socket for remote web server connection
	if ((remoteservfd = socket(AF_INET, SOCK_STREAM, 0)) == -1)
	{
		printf("\nError in remote server socket()");
		exit(0);
	}

	// Connecting to the remote server
	if (connect(remoteservfd, p->ai_addr, p->ai_addrlen) == -1)
	{
		close(remoteservfd);
		printf("\nError in connect() to remote server");
		exit(0);
	}
	
	// Sending GET request to the remote server
	bzero(sendline, 1024);
	strcpy(sendline, command);
	if (write(remoteservfd, sendline, strlen(sendline)) < 0)
	{
		printf("\n Error in write");
		exit(0);
	}

	//Generating filename
	fprintf(stderr, "\nGenerating html file...");
	strcpy(html, ".html");
	strcpy(htmlfile, "fileop_srv");
	sprintf(socknum, "%d", current + 1);
	strcat(htmlfile, socknum);
	strcat(htmlfile, html);
	fprintf(stderr, "\n%s", htmlfile);
	fp = fopen(htmlfile, "w+");

	// Reading data from the remote web server and writing it to client socket
	while ( (n = read(remoteservfd, recvline, 1024)) > 0)
	{
		recvline[n] = 0;
		write(sockfd, recvline, strlen(recvline));
		fputs(recvline, fp);
		bzero(recvline, 1024);
	}
	finishfd[current] = 1;

	fclose(fp);
	close(sockfd);
	close(remoteservfd);
	fprintf(stderr, "\ncurrent : %d, finish : %d", current, finishfd[current]);
	return NULL;
}

void GetHttpRequest(char line[], char* command1, char* host)
{
	char token[6], hostname[100], port[6], filename[100];
	char command[512];
	int i=0, j;
	strcpy(command, command1);

	while(1)
	{
		j = 0;
		strncpy(token, &line[i], 6);
		i += 6;

		if(strcmp(token, "host: ") == 0)
		{
			bzero(hostname, 100);
			//Read till next ' ' and get hostname
			while(line[i] != ' ')
			{
				hostname[j] = line[i];
				i++;j++;
			}
			hostname[j] = '\0';
			strcpy(host, hostname);
			i++;
		}
		else if(strcmp(token, "port: ") == 0)
		{
			bzero(port, 6);
			//Read till next ' ' and get port number
			while(line[i] != ' ')
			{
				port[j] = line[i];
				i++; j++;
			}
			port[j] = '\0';
			i++;
		}
		else if(strcmp(token, "file: ") == 0)
		{
			bzero(filename, 100);
			//Read till the end and get filename
			while(line[i] != '\0')
			{
				filename[j] = line[i];
				i++; j++;
			}
			filename[j] = '\0';
			break;
		}
	}
	bzero(command, 512);
	strcat(command, GET);
	strcat(command, filename);
	strcat(command, HTTP_GET_PARAM);
	strcat(command, hostname);
	strcat(command, SPACES);
	strcpy(command1, command);
}

