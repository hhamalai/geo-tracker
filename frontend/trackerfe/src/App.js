import React, { Component } from 'react';
import { render } from 'react-dom';
import { Map, Marker, Popup, TileLayer } from 'react-leaflet';

import './App.css';
import Objects from './Objects'

let FIXED_STARING_POSITION = [60.421858399999995, 23.455625799999997];
let DEFAULT_ZOOM_LEVEL = 8;

class App extends Component {
	constructor(props) {
    super(props);
    this.position = FIXED_STARING_POSITION;
    this.state = {user: '', objects: {}, userSet: false};
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }
  componentDidMount() {
    this.connection = new WebSocket('ws://' + window.location.host + ':8776/ws');
    this.connection.onmessage = evt => {
      let result = JSON.parse(evt.data);
      let newState = this.state;
			let newStateObjects = newState.objects;
			if (result.objectId) {
        newStateObjects[result.objectId] = result;
        newState.objects = newStateObjects;
	      this.setState(newState);
  	  }
    };
  }
  handleChange(event) {
    let state = this.state;
    state.user = event.target.value;
    this.setState(state);
  }
  handleSubmit(event) {
    event.preventDefault();
    this.connection.send("/user " + event.target.value);
    let state = this.state;
    state.userSet = true;
    this.setState(state);

    if ("geolocation" in navigator) {
      let geoOptions = {
        enableHighAccuracy: true
      };
      let conn = this.connection;
      navigator.geolocation.watchPosition(function(position) {
        if (state.userSet) {
          conn.send("/location " + JSON.stringify({
              "objectId": state.user,
              "eventTime": (new Date(position.timestamp)).toString(),
              "latitude": position.coords.latitude,
              "longitude": position.coords.longitude,
              "velocity": position.coords.speed
            }))
        } else {
          console.log("user missing, not updating")
        }
      }, function(positionError) {
        console.log("watchPosition error: ", positionError.code)
      }, geoOptions);
    } else {
      console.log("geolocation service not available on client")
    }
  }

  render() {
    let markers = Object.values(this.state.objects).map(function(trackedObject, index) {
      let position = [trackedObject.latitude, trackedObject.longitude];
      return (
        <Marker position={position} key={index}>
          <Popup>
            <span>{trackedObject.objectId}<br/>{trackedObject.velocity}</span>
          </Popup>
        </Marker>
      )
    });

    return (
      <div className="App">
        <div className="row">
          <div className="col-md-8" id="mapid">
            <Map center={this.position} zoom={DEFAULT_ZOOM_LEVEL} style={{height: "100vh"}}>
              <TileLayer
                url='http://{s}.tile.osm.org/{z}/{x}/{y}.png'
                attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
              />
              {markers}
            </Map>
          </div>
          <div className="col-md-4" id="metersid">
            {!this.state.userSet && 
            <form className="form-inline" onSubmit={this.handleSubmit}>
              <div className="form-group">
                <label>
                  Name:
                  <input value={this.state.user} onChange={this.handleChange} className="form-control" type="text" name="name" />
                </label>
                <input className="btn btn-default" type="submit" value="Submit" />
              </div>
            </form>
            }
            {this.state.userSet &&
              <Objects trackedObjects={this.state.objects}>
              </Objects>
            }
          </div>
        </div>
      </div>
    );
  }
}

export default App;
