(function() {
    var app = angular.module('athena');

    app.controller('DashboardSearchCtrl', ['$scope', 'GetCategories', function($scope, GetCategories) {
        $scope.categories = null;

        console.log("made it!");

        refreshCategories();

       function refreshCategories() {
            if ($scope.categories != null) {

            } else {
                $scope.myPromise = GetCategories.create().$promise.then(function(res){
                    var categories = [];
                    for (i = 0; i < res.body.length; i++) {
                        categories.push({ text: res.body[i]})
                    }
                    $scope.categories = categories;
                    //console.log(categories);
                    $scope.categories = categories;
                });
                return $scope.myPromise;
            }
        };

    }]);
})();
