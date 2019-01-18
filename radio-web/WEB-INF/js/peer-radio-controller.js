/**
 * PeerRadioController: peer radio controller.
 * Copyright (c) 2018 Marcel Trattner
 */
"use strict";

(function () {
	// imports
	const Controller = de_sb_radio.Controller;


	/**
	 * Creates a new welcome controller that is derived from an abstract controller.
	 */
	const PeerRadioController = function () {
		Controller.call(this);
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
	 * Peer to peer access
	 */
	Object.defineProperty(PeerRadioController.prototype, "initPeerAccess", {
		enumerable: false,
		configurable: false,
		writable: false,
		value: async function () {
			  var constraints = {
    		 	video: false,
    			audio: true,
 				 };
			  if(navigator.mediaDevices.getUserMedia) {
				    try {
				       let stream =	await navigator.mediaDevices.getUserMedia(constraints);
				   	   this.startTopTrack(stream);
				    } catch (error) {
				    	this.errorHandler(error);
				    }
			  } else {
			    alert('Your browser does not support getUserMedia API');
			  }						
		}
	});
	
	
			/**
	 * Peer to peer access
	 */
	Object.defineProperty(PeerRadioController.prototype, "errorHandler", {
		enumerable: false,
		configurable: false,
		writable: false,
		value: function (error) {
			console.log(error);
		}

	});
	
	
	
	
			/**
	 * Get UserMedia
	 */
	Object.defineProperty(PeerRadioController.prototype, "getUserMediaSuccess", {
		enumerable: false,
		configurable: false,
		writable: false,
		value: function () {
						
		}
	});
	
	
	
	
	/**
	 * Displays the sender section view.
	 */
	Object.defineProperty(PeerRadioController.prototype, "displaySenderSection", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: function () {
		    
			let mainElement = document.querySelector("main");
			while (mainElement.childElementCount > 1) {
				mainElement.removeChild(mainElement.lastChild);
			}

			let sectionElement = document.querySelector("#peer-radio-sender-template").content.cloneNode(true).firstElementChild;
			let inputElement = sectionElement.querySelector("input");
			inputElement.addEventListener("change", event => this.addToPlaylist(event.target.files), false);
			let buttonElement = sectionElement.querySelector("button");
			buttonElement.addEventListener("click", event => this.removeFromPlaylist());	
			mainElement.appendChild(sectionElement);
		}
	});
	
		
	/**
	 * Displays the receiver section view.
	 */
	Object.defineProperty(PeerRadioController.prototype, "displayReceiverSection", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: function () {
			let mainElement = document.querySelector("main");
			while (mainElement.childElementCount > 1) {
				mainElement.removeChild(mainElement.lastChild);
			}
			// TODO: to implement display receiver section.
			console.log(Controller.audioSource);
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
			
			if (empty && paths.length > 0) {
			//	this.startTopTrack();
			 	this.initPeerAccess();	// async function call, wird nicht sofort ausgeführt
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
				let audioSource = Controller.audioContext.createMediaStreamSource(stream);
				audioSource.loop = false;
				audioSource.connect(Controller.audioContext.destination);
				// TODO: Kann man aus audio context / audioSource ein event call back registrieren für das ende des Liedes. 				
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