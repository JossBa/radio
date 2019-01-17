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
		
		let localAudioContext = new AudioContext();
		Object.defineProperty(this, "audioContext", {
			enumerable: true,
			configurable: false,
			get: function () { 
				return localAudioContext;
			}	
		});
		
		let localPlaylist = [];
		Object.defineProperty(this, "playlist", {
			enumerable: true,
			configurable: false,
			get: function () { 
				return localPlaylist;
			}	
		});
		
		Object.defineProperty(this, "position", {
			enumerable: true,
			configurable: false,
			writable: true,
			value: -1
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
				this.startTopTrack();
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
		value: async function () {
				let optionElement = document.querySelector("main select.playlist option");
		 		const recordingFile = optionElement.filePath;
				const audioBuffer = await readAsArrayBuffer(recordingFile);
				const decodedBuffer = await this.audioContext.decodeAudioData(audioBuffer);
				let audioSource = this.audioContext.createBufferSource();
				audioSource.loop = false;
				audioSource.buffer = decodedBuffer;
				audioSource.connect(this.audioContext.destination);
				audioSource.start();
				// TODO: Kann man aus decoded buffer die Audiolänge abfragen oder ermitteln?
				// wenn ja, Länge zurückgeben. (vorzugsweise ms); Callback registrieren.
				// TODO: setTimeout(function() { your_func(); }, 5000); <Die Länge der Track. Aufgrund byte array. kann man decodieren.
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