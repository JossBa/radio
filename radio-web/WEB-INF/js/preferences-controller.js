/**
 * PreferencesController: radio preferences controller.
 * Copyright (c) 2018 Marcel Trattner
 */
"use strict";

(function () {
	// imports
	const Controller = de_sb_radio.Controller;


	/**
	 * Creates a new welcome controller that is derived from an abstract controller.
	 */
	const PreferencesController = function () {
		Controller.call(this);
	}
	PreferencesController.prototype = Object.create(Controller.prototype);
	PreferencesController.prototype.constructor = PreferencesController;


	/**
	 * Displays the associated view.
	 */
	Object.defineProperty(PreferencesController.prototype, "display", {
		enumerable: false,
		configurable: false,
		writable: true,
		value: function () {
			if (!Controller.sessionOwner) {
				const anchor = document.querySelector("header li:first-of-type > a");
				anchor.dispatchEvent(new MouseEvent("click")); 
				return;
			}
		}
	});
	
	/**
	 * Perform controller callback registration during DOM load event handling.
	 */
	window.addEventListener("load", event => {
		const anchor = document.querySelector("header li:nth-of-type(4) > a");
		const controller = new PreferencesController();
		anchor.addEventListener("click", event => controller.display());
	});
} ());