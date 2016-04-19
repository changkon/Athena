(function() {
    var app = angular.module('athena');

    app.controller('DashboardGroupsCtrl', ['$scope', 'InsertGroupPost', 'GetGroups', '$timeout', function($scope, InsertGroupPost, GetGroups, $timeout) {
        $scope.groups = [];

        refreshGroups();

       $scope.uploadGroup = function(){
           if ($scope.groupName == "" || $scope.groupName == undefined || $scope.emailEntries == "" || $scope.emailEntries == undefined) {
                console.log("Name or emails is missing")
                return;
           }
           var group = {name: $scope.groupName, description: $scope.description, memberEmails: $scope.emailEntries.split(/\s*[\s,]\s*/), ownerId: $scope.accountName}

           $scope.myPromise = InsertGroupPost.create({}, group).$promise.then(function(res){
               window.alert("Group " + $scope.groupName + " added!");
               $scope.groupName = "";
               $scope.description = "";
               $scope.emailEntries = "";
               refreshGroups();
           });
       }

       function refreshGroups() {
            console.log("account id is " + $scope.accountName);
            $scope.myPromise = GetGroups.create({id: $scope.accountName}, null).$promise.then(function(res){
                   console.log(res);
                   $scope.groups = res.body;
              });
       }

    }]);

})();
