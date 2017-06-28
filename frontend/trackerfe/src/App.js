import React, { Component } from 'react';
import { render } from 'react-dom';
import { Map, Marker, Popup, TileLayer } from 'react-leaflet';
import Websocket from 'react-websocket';

import './App.css';
import Objects from './Objects'

class App extends Component {
	constructor(props) {
    super(props)
    this.position = [51.505, -0.09];
    this.state = {user: '', objects: {}};
    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }
  handleChange(event) {
    this.setState({user: event.target.value});
  }
  handleSubmit(event) {
    alert('A name was submitted: ' + this.state.user);
    event.preventDefault();
  }
  handleData(data) {
	  console.log(data)
    let result = JSON.parse(data);
    let newState = this.state.objects;
    console.log("handle")
    if (result.objectId) {
      newState[result.objectId] = result;
      this.setState({objects: newState, user: this.state.user});
    }
  }

  render() {
    var markers = Object.values(this.state.objects).map(function(trackedObject, index) {
      let position = [trackedObject.latitude, trackedObject.longitude];
      console.log("pos", position)
      return (
        <Marker position={position} key={index}>
          <Popup>
            <span>{trackedObject.objectId}<br/>{trackedObject.velocity}</span>
          </Popup>
        </Marker>
      )
    })
    return (
      <div className="App">
        <div className="row">
          <div className="col-md-8" id="mapid">
            <Map center={this.position} zoom={13} style={{height: "100vh"}}>
              <TileLayer
                url='http://{s}.tile.osm.org/{z}/{x}/{y}.png'
                attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
              />
              {markers}
            </Map>
          </div>
          <Websocket url='ws://localhost:8776/ws' onMessage={this.handleData.bind(this)}/>
          <div className="col-md-4" id="metersid">
            <form className="form-inline" onSubmit={this.handleSubmit}>
              <div className="form-group">
                <label>
                  Name:
                  <input value={this.state.user} onChange={this.handleChange} className="form-control" type="text" name="name" />
                </label>
                <input className="btn btn-default" type="submit" value="Submit" />
              </div>
            </form>
            <Objects trackedObjects={this.state.objects}>
            </Objects>
          </div>
        </div>
      </div>
    );
  }
}

export default App;
