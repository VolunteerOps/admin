"use client";
import {Component,type ReactNode} from 'react';
import {Button,Notice} from './ui';
export default class ViewBoundary extends Component<{children:ReactNode},{failed:boolean}> {
 state={failed:false};
 static getDerivedStateFromError(){return {failed:true};}
 render(){return this.state.failed?<section className="panel panel-body"><h2>This section could not open</h2><p>Your workspace is still available. Try this section again or choose another page from the menu.</p><Button onClick={()=>this.setState({failed:false})}>Try again</Button></section>:this.props.children;}
}
