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
			let inputElement = sectionElement.querySelector("input");
			inputElement.addEventListener("change", event => this.pushPlaylist(event.target.files), false);
			let buttonElement = sectionElement.querySelector("button:nth-of-type(2)");
			buttonElement.addEventListener("click", event => this.addToPlaylist());	
			mainElement.appendChild(sectionElement);
		}
	});
	
	
	Object.defineProperty(PeerRadioController.prototype, "pushPlaylist", {
		enumerable: false,
		configurable: false,
		value: function (paths) {
			let selectElement = document.querySelector("main select.playlist");
		
			for (let path of paths) {
				let optionElement = document.createElement("option");
				optionElement.appendChild(document.createTextNode(path.name));
				optionElement.value = path;
				selectElement.appendChild(optionElement);
			}
		}
	});
	

	Object.defineProperty(PeerRadioController.prototype, "addToPlaylist", {
		enumerable: false,
		configurable: false,
		value: async function () {
				let inputElement = document.querySelector("section.radio-peer > input");
				let files = inputElement.files;
				for (let i = 0; i < files.length; ++i) {
					this.playlist.push(files.item(i));
				}
				
				this.startCurrentTrack();
		}
	});


	Object.defineProperty(PeerRadioController.prototype, "startCurrentTrack", {
		enumerable: false,
		configurable: false,
		value: async function () {
		 		const recordingFile = this.playlist[++this.position];
				const audioBuffer = await readFile(recordingFile);
				const decodedBuffer = await this.audioContext.decodeAudioData(audioBuffer);
				let audioSource = this.audioContext.createBufferSource();
				audioSource.loop = false;
				audioSource.buffer = decodedBuffer;
				audioSource.connect(this.audioContext.destination);
				audioSource.start();
				// TODO: Kann man aus decoded buffer die Audiolänge abfragen oder ermitteln?
				// wenn ja, Länge zurückgeben. (vorzugsweise ms); Callback registrieren.
		}
	});
	
	
	function readFile (inputFile) {
		const fileReader = new FileReader();
		return new Promise((resolve, reject) => {
			fileReader.onerror = () => {
				fileReader.abort();
				reject(new Error("Problem reading file."));
			};
	
			fileReader.onload = () => {
				resolve(fileReader.result);
			};
			fileReader.readAsArrayBuffer(inputFile);
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