<p align="center">
<img src="http://upload.wikimedia.org/wikipedia/commons/d/dc/Broadcast.svg" alt="Logo" height="120" />
</p>
WORK IN PROGRESS
# Dynode Broadcaster
The Dynode Broadcaster is a TCP replication server, or broadcaster, that replicates TCP commands to other TCP servers. Built with Java and Netty, it's super fast, and enterprise ready.

Created, sponsored and used in production by [Mashape](https://www.mashape.com), the Cloud API Hub.

### Features

* Set the final backend servers from a simple configuration file
* Optionally **auto-update** backend servers from a third-party HTTP resource
* Automatic handling of downtimes, network failures and reconnections
* Configurable timeout parameters
* Read the responses from all the backend servers, or skip all of them (useful in *write-and-forget* scenarios)



## Use Cases

There are many different use case scenarios where the Dynode Broadcaster can work well. For example, it can be used to create an eventual consistent cluster of Redis machines: to replicate counters, data, or implement an eventual consistent distributed caching cluster across a LAN/WAN.

* You have two Redis servers listening at `redis1.us:6379` and `redis2.eu:6379`.
* You can start the TCP broadcaster on `broadcaster.us:6379`

The Dynode Broadcaster replicates any command sent to `broadcaster.us:6379` to both `redis1.us:6379` and `redis2.eu:6379`. 

If you have a server `application1.us` in the same region of `redis1.us`, you could execute all the reads directly from `redis1.us:6379` and submit all the writes to `broadcaster.us:6379`, so that an other server `application2.eu` could read the same data in his own region from `redis2.eu:6379`.

# Usage

lin

# Configuration

```
# Dynode Broadcaster configuration file

# Backend Servers configuration
#
# You can explicitly set the backend servers with the "servers" property. 
# both the host and the port are mandatory.
#
# the servers must be comma separated "host:port" values, like: "servers=127.0.0.1:8000, 127.0.0.2:8001"
servers=127.0.0.1:6379

################################ AUTO-UPDATE  #################################
# Instead, or in addition, of explicitly set the backend servers, you can also set an HTTP endpoint that will load
# the backend servers. To enable this functionality set "servers_autoupdate=true"
#
# you can specify the HTTP method, the URL and any additional parameter to be sent with the request
#
# the servers will be automatically refreshed by the seconds timeout specified in "servers_autoupdate_refresh"
# or not refreshed at all if the property is set to "0" zero.
servers_autoupdate=false

# The HTTP methods available are GET, POST, PUT, PATCH, DELETE, although only GET and POST are strongly reccomended.
servers_autoupdate_method=POST

# The URL that will return a JSON array containing the servers addresses, like:
# [ "127.0.0.1:10000", "127.0.0.1:20000", "127.0.0.1:30000" ]
servers_autoupdate_url=http://example.com

# Any additional parameters to send along with the request, for example:
# servers_autoupdate_parameters=username=admin&password=hello&role=admin%20user
#
# Be sure to properly encode the parameters
servers_autoupdate_parameters=

# The amount of seconds after which the servers will be repeatedly refreshed by making the HTTP request
#
# set to "0" zero to never refresh the servers after the first request.
servers_autoupdate_refresh=10

############################## GENERAL SETTINGS ###############################
# The host and port where the server should start
host=localhost

# Check that you have permissions to run on the specified port
port=9000

############################## NETWORK SETTINGS ###############################
backlog_size=128
reuse_addr=true

# By default Dynode Broadcaster returns all the responses from all the servers
# to skip the responses if not needed, and increase system performance, set this property to "discard_responses=true"
discard_responses=false

############################## TIMEOUT SETTINGS ###############################
# The connection timeout to a backend server in seconds
connect_timeout=5

# The time to wait in seconds before re-attempting the connection
reconnect_delay_timeout=10

# If no data was received from the connected client during the "read_idle_timeout", 
# the connection with the client will be closed.
read_idle_timeout=60
```
