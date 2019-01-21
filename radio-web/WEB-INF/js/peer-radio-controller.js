/**
 * PeerRadioController: peer radio controller.
 * Copyright (c) 2018 Marcel Trattner
 */
"use strict";

(function () {
	const Controller = de_sb_radio.Controller;
	const TEN_MINUTES = 10 * 60 * 1000;

	/**
	 * Creates a new welcome controller that is derived from an abstract controller.
	 */
	const PeerRadioController = function () {
		Controller.call(this);
		
		Object.defineProperty(this, "rtcConnection", {
			enumerable: true,
			configurable: false,
			value: new RTCPeerConnection()
		});
	}
	PeerRadioController.prototype = Object.create(Controller.prototype);
	PeerRadioController.prototype.constructor = PeerRadioController;


	/**
	 * Displays the associated view.
	 */
	Object.defineProperty(PeerRadioController.prototype, "display", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: function () {
			if (!Controller.sessionOwner) {
				const anchor = document.querySelector("header li:first-of-type > a");
				anchor.dispatchEvent(new MouseEvent("click")); 
				return;
			}
			
			
			let mainElement = document.querySelector("main");
			let sectionElement = document.querySelector("#peer-radio-template").content.cloneNode(true).firstElementChild;
			let buttonElements = sectionElement.querySelectorAll("button");
			buttonElements[0].addEventListener("click", event => this.displaySenderSection());
			buttonElements[1].addEventListener("click", event => this.displayReceiverSection());
			mainElement.appendChild(sectionElement);
		}
	});
	
	
	/**
	 * Displays the sender section view.
	 */
	Object.defineProperty(PeerRadioController.prototype, "displaySenderSection", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: async function () {
		    
			let mainElement = document.querySelector("main");
			while (mainElement.childElementCount > 1) {
				mainElement.removeChild(mainElement.lastChild);
			}

			let sectionElement = document.querySelector("#peer-radio-sender-template").content.cloneNode(true).firstElementChild;
			let inputElement = sectionElement.querySelector("input");
			inputElement.addEventListener("change", event => this.addToPlaylist(event.target.files));
			let buttonElement = sectionElement.querySelector("button");
			buttonElement.addEventListener("click", event => this.removeFromPlaylist());	
			mainElement.appendChild(sectionElement);
			
			const connectionOffer = await this.rtcConnection.createOffer({iceRestart: false, offerToReceiveAudio: true});
			const transmission = { timestamp: Date.now(), address: null, offer: JSON.stringify(connectionOffer) };
			Controller.sessionOwner.lastTransmission = transmission;
			
			const body = JSON.stringify(Controller.sessionOwner);
			let response = await fetch("/services/people", { method: "POST", credentials: "include", headers: { "Content-Type": "application/json"}, body: body });
			if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
			Controller.sessionOwner.version += 1;

			/*if(navigator.mediaDevices.getUserMedia) {
				    try {
				       let stream =	await navigator.mediaDevices.getUserMedia(constraints);
				   	   this.startTopTrack(stream);
				    } catch (error) {
				    	this.errorHandler(error);
				    }
			  } else {
			    alert('Your browser does not support getUserMedia API');
			  }
			  */
			  			
		}
	});
	
		
	/**
	 * Displays the receiver section view.
	 */
	Object.defineProperty(PeerRadioController.prototype, "displayReceiverSection", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: async function () {
			let mainElement = document.querySelector("main");
			while (mainElement.childElementCount > 1) {
				mainElement.removeChild(mainElement.lastChild);
			}
			
			const body = JSON.stringify(Controller.sessionOwner);
			const uri = "/services/people?lastTransmissionTimestamp=" + (Date.now() - TEN_MINUTES);
			let response = await fetch(uri, { method: "GET", credentials: "include", headers: { Accept: "application/json"}});
			if (!response.ok) throw new Error("HTTP " + response.status + " " + response.statusText);
			const people = await response.json();
			
			let sectionElement = mainElement.querySelector("section:last-of-type");
			
			for (let person of people) {
				let anchorElement = document.createElement("a");
				anchorElement.appendChild(document.createTextNode(person.forename + " " + person.surname));
				anchorElement.addEventListener("click", event => this.displayListenSection(person));
				sectionElement.appendChild(anchorElement);	
			}
			
			
			/*
			// TODO: to implement display receiver section.
			let audioSource = Controller.audioContext.createBufferSource();
				audioSource.loop = false;
				audioSource.buffer = decodedBuffer;
				audioSource.connect(Controller.audioContext.destination);
				audioSource.start();
				*/
		}
	});
	
	
	Object.defineProperty(PeerRadioController.prototype, "displayListenSection", {
		enumerable: false,
		configurable: false,
		value: function (person) {
			const offerTemplate = JSON.parse(person.lastTransmission.offer);
			let offer = new RTCSessionDescription({ type: "offer" });
			for (let key of Object.keys(offerTemplate)) {
				offer[key] = offerTemplate[key];
			}
			
			console.log(offer);
		}
	});
	
		
	
	Object.defineProperty(PeerRadioController.prototype, "addToPlaylist", {
		enumerable: false,
		configurable: false,
		value: function (paths) {
			let selectElement = document.querySelector("main select.playlist");
			let empty = selectElement.querySelectorAll("option").length == 0;
			
			for (let path of paths) {
				let optionElement = document.createElement("option");
				optionElement.appendChild(document.createTextNode(path.name));
				optionElement.filePath = path;
				selectElement.appendChild(optionElement);
			}
			
			if (empty & paths.length > 0) {
			//	this.startTopTrack();
			 	
			} 
			
		}
	});
	

	Object.defineProperty(PeerRadioController.prototype, "removeFromPlaylist", {
		enumerable: false,
		configurable: false,
		value: function () {
			let selectElement = document.querySelector("main select.playlist");
			let optionElements = selectElement.querySelectorAll('option:checked');
			
			for (let optionElement of optionElements) {
				selectElement.removeChild(optionElement);
			}	
		}
	});

	Object.defineProperty(PeerRadioController.prototype, "startTopTrack", {
		enumerable: false,
		configurable: false,
		value: async function (stream) {
				let optionElement = document.querySelector("main select.playlist option");
		 		const recordingFile = optionElement.filePath;
				const audioBuffer = await readAsArrayBuffer(recordingFile);
				const decodedBuffer = await Controller.audioContext.decodeAudioData(audioBuffer);
				let audioSource = Controller.audioContext.createBufferSource();
				audioSource.loop = false;
				audioSource.buffer = decodedBuffer;
				audioSource.connect(Controller.audioContext.destination);
				audioSource.start();
				
				// TODO: Kann man aus audioContext / audioSource ein event call back registrieren für das ende des Liedes. 				
		}
	});
	
	
	/**
	 * Returns a promise of array buffer content read from the given file,
	 * which can be evaluated using the await command. The latter throws an
	 * error if reading said file fails.
	 * @param {File} file the file to be read
	 * @return {Promise} the promise of array buffer content read from the given file
	 */
	function readAsArrayBuffer (file) {
	    return new Promise((resolve, reject) => {
	        let reader = new FileReader();
	        reader.onload = () => resolve(reader.result);
	        reader.onerror = reject;
	        reader.readAsArrayBuffer(file);
	    });
	}
	
	
	/**
	 * Perform controller callback registration during DOM load event handling.
	 */
	window.addEventListener("load", event => {
		const anchor = document.querySelector("header li:nth-of-type(3) > a");
		const controller = new PeerRadioController();
		anchor.addEventListener("click", event => controller.display());
	});
} ());