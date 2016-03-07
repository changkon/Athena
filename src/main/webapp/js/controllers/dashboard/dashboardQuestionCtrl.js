(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','Post', function($scope, Post) {

        $scope.uploadFile = function(){
            var file = $scope.myFile;
            console.log("file recived");
            console.log(file);
            var fd = new FormData();
            fd.append('uploadedFile', file);

            Post.create({}, fd).$promise.then(function(res){
                console.log(res);
            }).catch(function (err) {
                console.log("error: ");
                console.log(err);
            });
        };
    }]);
})();
