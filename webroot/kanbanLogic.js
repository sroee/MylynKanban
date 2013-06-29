/*jslint white: true, nomen: true, plusplus:true, browser: true */
/*global angular, _*/

(function() {
	"use strict";
	
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
							(task.data.category === $scope.category || $scope.category === "" && !task.data.category);
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
		} else {
			return str;
		}
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
			endDate: task.endDate,
			estimated: estimated,
			category: putEllipsis(task.category, 25)
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

	
}());