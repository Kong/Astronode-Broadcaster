<p align="center">
<img src="http://upload.wikimedia.org/wikipedia/commons/d/dc/Broadcast.svg" alt="Logo" height="120" />
</p>
WORK IN PROGRESS
# Dynode Broadcaster
The Dynode Broadcaster is a TCP replication server, or broadcaster, that replicates TCP commands to other TCP servers. Built with Java and Netty, it's super fast, and enterprise ready.

Created, sponsored and used in production by [Mashape](https://www.mashape.com), the Cloud API Hub.

### Features

* Offload TCP connections from your client apps to the broadcaster. It can handle hundreds of backend connections, simplifying broadcasting data to servers just by opening one connection client-side.
* Automatically handles backend connection downtimes, network failures and reconnections.
* Optionally **auto-update** backend servers from one single configurable HTTP endpoint, useful for to hot configure the broadcaster remotely without restarting it.
* Forwards back the responses from all the backend servers, or optionally skips them all: useful in *write-and-forget* scenarios.
* Tunable network and timeout settings.
* Very fast, and easy to use.

## Usage

```
java -jar dynode-broadcaster.jar -c ./configuration
```

## Use Cases

There are many different use case scenarios where the Dynode Broadcaster can work well. It can be used to create eventual consistent distributed clusters for those services that don't support clustering. For example you can use it with in-memory stores like Redis or Memcached to replicate any kind of data, counters, or implement an eventual consistent distributed caching cluster across a LAN/WAN. 

For example:

* You have two application servers in two different world regions: `app.us` and `app.eu`
* You also have one Redis server for each region: `redis.us:6379` and `redis.eu:6379`
* The application servers need to talk with Redis to load cached content. `app.us` will read from `redis.us` and `app.eu` will read from `redis.eu` to reduce network latency
* Every application reads data from the closest Redis server, but broadcasts the writes to the other region by using Dynode Broadcaster
* You can setup two Dynode Broadcasters in each region: `broadcaster.us:6379` and `broadcaster.eu:6379`
* You configure both broadcasters with the following backend servers: `redis.us:6379` and `redis.eu:6379`

With this configuration, any command sent to `broadcaster.us:6379` is broadcasted to both `redis1.us:6379` and `redis2.eu:6379`. The same happens for every command sent to `broadcaster.eu:6379`.

The server `app.us` is in the same region of `redis.us`. Now you could execute all the reads directly from `redis.us:6379` and submit all the writes to `broadcaster.us:6379`, so that an other server `app.eu` could read the same data in his own region from `redis.eu:6379`. Basically the app servers are using Redis for reading, and Dynode Broadcaster for writing.

If you need to broadcast data to, let's say, 100+ Redis machines, open just one connection to the broadcaster and let him do the hard work for you. And with the auto-update feature, you can hot-configure the broadcaster remotely without reloading it.

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
