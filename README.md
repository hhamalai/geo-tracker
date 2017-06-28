# Cassandra backed geolocation server with websocket API.

## Cassandra configuration
```
CREATE KEYSPACE tracker 
    WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };
CREATE TABLE tracker.location (
    object_id varchar, 
    event_time timestamp, 
    latitude float, 
    longitude float, 
    velocity float, 
    PRIMARY KEY (object_id, event_time));
CREATE TABLE tracker.chat(
    message_id varchar, 
    event_time timestamp,
    message varchar, 
    PRIMARY KEY (message_id, event_time));
```

##JSON command messages

Update location
```{
    "lat" :: double // latitude
    "lon" :: double // longitude
    "vel" :: double // velocity
}
```
