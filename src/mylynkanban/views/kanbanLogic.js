/*jslint white: true, nomen: true, plusplus:true, browser: true */
/*global angular, _*/

(function() {
	"use strict";
	
	angular.module('kanban', []).directive('column', function() {
		return {
			restrict: 'E',
			scope : {
				status: '@',
				taskList: '=taskList'
			},
			templateUrl: "tmpl/column.html",
			replace: true
		};
	}).directive('lane', function() {
		return {
			restrict: 'E',
			scope: { taskList: '=taskList' },
			templateUrl: "tmpl/lane.html",
			replace: true
		};
	});

	function TaskConnector() {
		var that = this,
			taskUpdateCallbacks = [],
			tasksIndex = {};
		
		that.tasksList = [];
		
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
			function insert(task) {
				var wrapped = {data: task};
				that.tasksList.push(wrapped);
				tasksIndex[task.id] = wrapped;
			}
			
			function update(updatedTask, task) {
				updatedTask.data = task;
			}
			
			if (task.id) {
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

	window.taskConnector = new TaskConnector();

	window.TaskListController = function($scope) {
		$scope.taskList = window.taskConnector.tasksList;
		window.taskConnector.addTaskModifiedListener(function() {
			$scope.$apply();
		});
		
		$scope.roeeTest = function(task) {
			return false;
		};
	};

	function detectStatus(completed, hasContext, isActive) {
		return completed ? "done" :	((hasContext || isActive) ? "inprogress" : "todo");
	}

	window.buildTask = function(task) {
		return {
			id: task.id, 
			summary: task.summary, 
			status: detectStatus(task.isCompleted, task.hasContext, task.isActive),
			dueDate: task.dueDate,
			startDate: task.startDate,
			endDate: task.endDate,
			estimated: task.estimated
		};
	};
	
}());

