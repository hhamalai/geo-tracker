# Cassandra backed geolocation server with websocket API.

## Cassandra configuration
```
CREATE KEYSPACE tracker WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };
CREATE TABLE tracker.location ( object_id int, event_time timestamp, latitude float, longitude float, PRIMARY KEY (object_id) );
```

##JSON command messages

Update location
```{
    "lat" :: double // latitude
    "lon" :: double // longitude
    "vel" :: double // velocity
}
```
