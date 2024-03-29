/*jslint white: true, nomen: true, plusplus:true, browser: true */
/*global angular, _*/

(function() {
	"use strict";
	
	function unsupported() {
		angular.module('kanban', []);
		window.location.href="notSupported.html";
	}
	
	function supported() {
		var HOURS_A_DAY = 8;
		var commands = {};
		
		angular.module('kanban', []).directive('column', function() {
			return {
				restrict: 'E',
				scope : {
					status: '@',
					taskList: '=taskList',
					category: '=category'
				},
				controller: function($scope, $element){ 
					$scope.testTask = function(task) {
						return 	(task.data.status === $scope.status) &&
								(task.data.category === $scope.category || ($scope.category === "" && !task.data.category));
					}; 
				},
				templateUrl: "tmpl/column.html",
				replace: true
			};
		}).directive('lane', function() {
			return {
				restrict: 'E',
				scope: { taskList: '=taskList',
						 category: '=category'},
				templateUrl: "tmpl/lane.html",
				replace: true
			};
		});

		function TaskConnector() {
			var that = this,
				taskUpdateCallbacks = [],
				tasksIndex = {};
			
			that.tasksList = [];
			that.categories = [];
			
			var notifyTaskListeners = function() {
				var idx;
				for (idx = 0; idx < taskUpdateCallbacks.length; idx++) {
					taskUpdateCallbacks[idx]();
				}
			};
			
			that.addTaskModifiedListener = function(listener) {
				taskUpdateCallbacks.push(listener);
			};
			
			that.upsertTask = function(task) {
				function ensureCategory(taskData) {
					if (taskData.category) {
						if (_.indexOf(that.categories, taskData.category) === -1) {
							that.categories.push(taskData.category);
						}
					} else if (_.indexOf(that.categories, "") === -1) {
						that.categories.push("");
					}
				}
				
				function insert(task) {
					var wrapped = {data: task};
					that.tasksList.push(wrapped);
					tasksIndex[task.id] = wrapped;
				}
				
				function update(updatedTask, task) {
					updatedTask.data = task;
				}
				
				if (task.id) {
					ensureCategory(task);
					if (tasksIndex[task.id]) {
						update(tasksIndex[task.id], task);
					} else {
						insert (task);
					}
					notifyTaskListeners();
				}
			};
			
			that.removeTask = function(id) {
				if (tasksIndex[id]) {
					var deleted = tasksIndex[id];
					delete tasksIndex[id];
					that.tasksList.splice(_.indexOf(that.tasksList, deleted), 1);
					notifyTaskListeners();
				}
			};
			return that;
		}

		var taskConnector = new TaskConnector();

		window.TaskListController = function($scope) {
			$scope.taskList = taskConnector.tasksList;
			$scope.categories = taskConnector.categories;
			taskConnector.addTaskModifiedListener(function() {
				$scope.$apply();
			});			
		};

		function detectStatus(completed, hasContext, isActive) {
			return completed ? "done" :	((hasContext || isActive) ? "inprogress" : "todo");
		}
		
		function putEllipsis(str, maxLength) {
			if (str.length > maxLength) {
				return str.substring(0, maxLength - 3) + "...";
			} 
			return str;
		}

		function buildTask(task) {
			var estimated = task.estimated;
			if (estimated) {
				estimated = Math.round( (estimated / HOURS_A_DAY) * 10 ) / 10;
			}
			return {
				id: task.id, 
				summary: task.summary, 
				status: detectStatus(task.isCompleted, task.hasContext, task.isActive),
				dueDate: task.dueDate,
				startDate: task.startDate,
				scheduledEndDate: task.scheduledEndDate,
				scheduledStartDate: task.scheduledStartDate,
				endDate: task.completionDate,
				estimated: estimated,
				category: putEllipsis(task.category, 25),
				getDueDateColor : function() {
					var colorToReturn = "blue";
					var currentTime = new Date().getTime();
					
					if (this.dueDate) {
						if (this.dueDate.time > currentTime) {
							colorToReturn = "green";
						} else {
							colorToReturn = "red";
						}
					}
					
					return {"background-color": colorToReturn};
				},
				getCompletedDateColor : function() {
					var colorToReturn = "blue";
					
					if (this.endDate) {
						if (this.dueDate) {
							if (this.dueDate.time >= this.endDate.time) {
								colorToReturn = "green";
							} else {
								colorToReturn = "#8C0000";
							}
						} else {
							colorToReturn = "green";
						}
					}
					
					return {"background-color": colorToReturn};
				},
				getStartedDateColor : function() {
					var colorToReturn = "blue";
					
					if (this.startDate) {
						var schedEndTime = this.scheduledStartDate.time + (2 * 24 * 60 * 60 * 1000)
						if (this.scheduledEndDate) {
							schedEndTime = this.scheduledEndDate.time;
						}
						
						if (this.startDate.time <= schedEndTime) {
							colorToReturn = "green";
						} else {
							colorToReturn = "#8C0000";
						}
					}
					
					return {"background-color": colorToReturn};
				},
				getScheduledDateColor : function() {
					var colorToReturn = "blue";
					var currentTime = new Date().getTime();
					if (this.scheduledStartDate) {
						var schedEndTime = this.scheduledStartDate.time + (2 * 24 * 60 * 60 * 1000)
						if (this.scheduledEndDate) {
							schedEndTime = this.scheduledEndDate.time;
						}
						
						if (this.status === 'todo') {
							if (this.scheduledStartDate.time > currentTime) {
								colorToReturn = "black";
							} else {
								if (schedEndTime >= currentTime) {
									colorToReturn = "green";
								} else {
									colorToReturn = "red";
								}
							}
						} else {
							var dateToCompare = this.startDate || this.endDate;
							if (dateToCompare) {
								if (dateToCompare.time <= schedEndTime) {
									colorToReturn = "green";
								} else {
									colorToReturn = "red";
								}
							}
							else {
								colorToReturn = "purple";
							}							
						}
					}
					
					return {"background-color": colorToReturn};
				}
			};
		};
		
		commands.upsert = function(tasksArr) {
			var i;
			for(i = 0; i < tasksArr.length; i++) {
				taskConnector.upsertTask(buildTask(tasksArr[i]));
			}
			return true;
		};
		
		commands.remove = function(taskId) {
			taskConnector.removeTask(taskId);
		};
		
		
		(function handleServerComm() {
			var connection = new WebSocket('ws://localhost:9999/service/kanban_tasks_api');
			
			// Log errors
			connection.onerror = function (error) {
			  console.log('WebSocket Error ' + error);
			};

			// Log messages from the server
			connection.onmessage = function (e) {
				var data = JSON.parse(e.data);
				if (data.command) {
					commands[data.command](data.params);
				}
			};
		}());	
	}
	
	
	var isSupported = (function() {
		
		// undefined is interpreted as false, otherwise true, 
		// returns undefined for false, otherwise the true element.
		function or() {
			var remain = _.without(arguments, undefined); 
			if (remain.length === 0)
				return undefined;
			return remain[0];
		}
		
		// undefined is interpreted as false, otherwise true, 
		// returns undefined for false, otherwise the true element.
		function xor() {
			var remain = _.without(arguments, undefined); 
			if (remain.length === 1)
				return remain[0];
			return undefined;
		}
		
		// undefined is false, otherwise true.
		function req(req) {
			return req !== undefined;
		}
		return	req(window.WebSocket) &&
				req(document.head) &&
		        req(or(document.head.style.MozBorderRadius, 
		        		document.head.style.WebkitBorderRadius, 
	                    document.head.style.borderRadius)) && 
	            req(or(document.head.style.boxShadow,
                    	document.head.style.WebkitBoxShadow,
                    	document.head.style.MozBoxShadow)) &&
                req(xor(document.head.style.zoom, document.head.style.MozTransform));		
	}());
	if (!isSupported) {
		unsupported();
	} else {
		supported();
	}
}());