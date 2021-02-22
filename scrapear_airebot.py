#! C:/..../python.exe

import cgi;
import cgitb;cgitb.enable()

import requests


def get_legends():	

	headers = {
	    'Connection': 'keep-alive',
	    'User-Agent': 'Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36',
	    'Accept': '*/*',
	    'Origin': 'http://airelib.re',
	    'Sec-Fetch-Site': 'cross-site',
	    'Sec-Fetch-Mode': 'cors',
	    'Sec-Fetch-Dest': 'empty',
	    'Referer': 'http://airelib.re/',
	    'Accept-Language': 'en-US,en;q=0.9,es;q=0.8',
	}

	response = requests.get('https://rald-dev.greenbeep.com/api/v1/aqi', headers=headers)

	return response.json()

legends = get_legends()

#print(legends)


print ("Content-Type: text/html")
print ("") 
print ("%s" % legends)
#print ("<h>%s</h>" % legends)



