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
			let inputElement = sectionElement.querySelector("input");
			inputElement.addEventListener("change", event => this.pushPlaylist(event.target.files), false);
			
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
	
	/**
	 * Perform controller callback registration during DOM load event handling.
	 */
	window.addEventListener("load", event => {
		const anchor = document.querySelector("header li:nth-of-type(3) > a");
		const controller = new PeerRadioController();
		anchor.addEventListener("click", event => controller.display());
	});
} ());