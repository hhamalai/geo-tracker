import React, { Component } from 'react';
import { render } from 'react-dom';

class TrackedObject extends Component {
  render() {
    return (
      <div className="panel panel-default">
        <div className="panel-heading">{this.props.status.objectId}</div>
        <div className="panel-body text-left">
          <b>Updated:</b> {this.props.status.eventTime}<br />
          <b>Latitude</b>: {this.props.status.latitude}<br />
          <b>Longitude</b>: {this.props.status.longitude}<br />
          <b>Velocity</b>: {this.props.status.velocity}<br />
        </div>
      </div>
    )
  }
}

class Objects extends Component {
  render() {
    console.log(this.props)
    var trackedObjects = Object.values(this.props.trackedObjects).map(function(trackedObject, index) {
      return (
        <TrackedObject status={trackedObject} key={index}>
        </TrackedObject>
      )
    })
    return (
      <div>
        {trackedObjects}
      </div>
    )
  }
}

export default Objects
