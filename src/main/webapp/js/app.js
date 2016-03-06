// Athena angularjs app
// loads config and routing
angular.module('athena', ['ui.router'])
.config(function($stateProvider, $urlRouterProvider) {
    $stateProvider
        .state('dashboard', {
            url: '',
            templateUrl: '/templates/dashboard.html',
            controller: 'DashboardCtrl'
        });

    // For any unmatched url, redirect to /dashboard
    $urlRouterProvider.otherwise("");
});