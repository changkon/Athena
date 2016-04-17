(function() {
    var app = angular.module('athena');

    app.controller('DashboardGroupsCtrl', ['$scope', 'InsertGroupPost', '$timeout', function($scope, InsertGroupPost, $timeout) {

       $scope.uploadGroup = function(){
           console.log("name is: " + $scope.groupName)
           console.log("description is: " + $scope.description)
           console.log("emails are: " + $scope.emailEntries.split(/\s*[\s,]\s*/))
           var group = {name: $scope.groupName, description: $scope.description, memberEmails: $scope.emailEntries.split(/\s*[\s,]\s*/)}

           $scope.myPromise = InsertGroupPost.create({}, group).$promise.then(function(res){
               console.log(res);
           });
       }

    }]);

})();
