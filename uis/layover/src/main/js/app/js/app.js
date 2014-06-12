'use strict';

// See http://stackoverflow.com/questions/5306680/move-an-array-element-from-one-array-position-to-another
Array.prototype.move = function (old_index, new_index) {
    if (new_index >= this.length) {
        var k = new_index - this.length;
        while ((k--) + 1) {
            this.push(undefined);
        }
    }
    this.splice(new_index, 0, this.splice(old_index, 1)[0]);
    return this; // for testing purposes
};

Array.prototype.each = function (callback) {
  for(var i=0; i<this.length; i++){
    callback(this[i]);
  }
  return this;
};

var providers = {};

// Declare app level module which depends on filters, and services
angular.module('aprox', [
  'ngRoute',
  'aprox.filters',
  'aprox.directives',
  'aprox.services',
  'aprox.controllers'
], function($controllerProvider, $compileProvider, $provide){
  providers = {
    $controllerProvider: $controllerProvider,
    $compileProvider: $compileProvider,
    $provide: $provide,
  };
}). // NOTE: NOT THE END OF A STATEMENT

// NOTE: In the routes below, the '#' route prefix is implied.
config(['$routeProvider', function($routeProvider) {
  if ( addons !== undefined ){
    addons.items.each( function(addon){
      if( addon.sections !== undefined ){
        addon.sections.each(function(section){
          var options = {};
          options.templateUrl= 'cp/layover/' + section.templateHref;

          if (section.controller !== undefined){
            options.controller= section.controller;
          }

          $routeProvider.when(section.route, options);
        });
      }
    });
  }

  $routeProvider.when('/remote', {templateUrl: 'partials/remote-list.html', controller: 'RemoteListCtl'});
  $routeProvider.when('/remote/view/:name', {templateUrl: 'partials/remote-detail.html', controller: 'RemoteDetailCtl'});
  $routeProvider.when('/remote/new', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteEditCtl'});
  $routeProvider.when('/remote/edit/:name', {templateUrl: 'partials/remote-edit.html', controller: 'RemoteEditCtl'});

  $routeProvider.when('/hosted', {templateUrl: 'partials/hosted-list.html', controller: 'HostedListCtl'});
  $routeProvider.when('/hosted/view/:name', {templateUrl: 'partials/hosted-detail.html', controller: 'HostedDetailCtl'});
  $routeProvider.when('/hosted/new', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedEditCtl'});
  $routeProvider.when('/hosted/edit/:name', {templateUrl: 'partials/hosted-edit.html', controller: 'HostedEditCtl'});

  $routeProvider.when('/group', {templateUrl: 'partials/group-list.html', controller: 'GroupListCtl'});
  $routeProvider.when('/group/view/:name', {templateUrl: 'partials/group-detail.html', controller: 'GroupDetailCtl'});
  $routeProvider.when('/group/new', {templateUrl: 'partials/group-edit.html', controller: 'GroupEditCtl'});
  $routeProvider.when('/group/edit/:name', {templateUrl: 'partials/group-edit.html', controller: 'GroupEditCtl'});

  $routeProvider.otherwise({redirectTo: '/remote'});
}]);
