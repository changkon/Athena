(function() {
    var app = angular.module('athena');

    app.controller('DashboardSearchCtrl', ['$scope', 'GetCategories', 'GetQuestions', '$location', function($scope, GetCategories, GetQuestions, $location) {
        $scope.categories = null;


        refreshCategories();

        $scope.tags = [];

        $scope.submit = function() {
            var categoryTagsDto = {categoryTags: []};
            for (var i = 0; i < $scope.tags.length; i++) {
                categoryTagsDto.categoryTags.push($scope.tags[i].text);
            }
            console.log("tags are: " + categoryTagsDto.categoryTags)
            $scope.myPromise = GetQuestions.create({}, categoryTagsDto).$promise.then(function(res){
               console.log(res);
               if (res.body.length > 0) {
                   $location.path("question");
                   $scope.$emit('setQuestions', res);
               } else {
                    alert("Sorry, no questions found.")
               }
            });
          };

        $scope.loadTags = function(query) {
            if ($scope.categories != null) {
                return $scope.categories.filter(function(category) {
                  return category.text.toLowerCase().indexOf(query.toLowerCase()) != -1;
                });
            } else {
                $scope.myPromise = GetCategories.create().$promise.then(function(res){
                    var categories = [];
                    for (i = 0; i < res.body.length; i++) {
                        categories.push({ text: res.body[i]})
                    }
                    $scope.categories = categories;
                  //  console.log(categories);
                    return categories.filter(function(category) {
                      return category.text.toLowerCase().indexOf(query.toLowerCase()) != -1;
                    });
                });
                return $scope.myPromise;
            }
        };

        $scope.checkTag = function(tag){
            return $scope.tags.length < 3;
        }

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
