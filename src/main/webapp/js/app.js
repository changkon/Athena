// Athena angularjs app
// loads config and routing
angular.module('athena', ['ui.router','ngResource','ui.bootstrap','cgBusy','ui.select', 'ngTagsInput'])
.config(function($stateProvider, $urlRouterProvider) {
    $stateProvider
        .state('dashboard', {
            abstract: true,
            templateUrl: '/templates/dashboard.html'
        })
        .state('dashboard.question', {
            url: '/question',
            templateUrl: '/templates/dashboard-question.html',
            controller: 'DashboardQuestionCtrl'
        });

    // For any unmatched url, redirect to /question
    $urlRouterProvider.otherwise("/question");
});