# Dynode Broadcaster

The Dynode Broadcaster is a TCP replication server, or broadcaster, that replicates TCP commands to other TCP servers. Built with Java and Netty, it's super fast, and enterprise ready. It automatically handles downtimes, network failures and reconnections.

## Features

* Set the final backend servers from a simple configuration file
* Optionally **auto-update** backend servers from a third-party HTTP resource
* Automatic handling of downtimes, network failures and reconnections
* Configurable timeout parameters
* Read the responses from all the backend servers, or skip all of them (useful in *write-and-forget* scenarios)

<div style="text-align: center;">
<img src="http://upload.wikimedia.org/wikipedia/commons/d/dc/Broadcast.svg" alt="Logo" style="height: 200px;" />
</div>

Created, sponsored and used in production by [Mashape](https://www.mashape.com), the Cloud API Hub.

## Use Cases

There are many different use case scenarios where the Dynode Broadcaster can work well. For example, it can be used to create an eventual consistent cluster of Redis machines: to replicate counters, data, or implement an eventual consistent distributed caching cluster across a LAN/WAN.

* You have two Redis servers listening at `redis1.us:6379` and `redis2.eu:6379`.
* You can start the TCP broadcaster on `broadcaster.us:6379`

The Dynode Broadcaster replicates any command sent to `broadcaster.us:6379` to both `redis1.us:6379` and `redis2.eu:6379`. 

If you have a server `application1.us` in the same region of `redis1.us`, you could execute all the reads directly from `redis1.us:6379` and submit all the writes to `broadcaster.us:6379`, so that an other server `application2.eu` could read the same data in his own region from `redis2.eu:6379`.

# Usage

lin

# Configuration
