(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', function($scope, FilePost) {

        $scope.uploadFile = function(){
            var file = $scope.myFile;
            console.log("file recived");
            console.log(file);
            var fd = new FormData();
            fd.append('uploadedFile', file);

            FilePost.create({}, fd).$promise.then(function(res){
                console.log(res);
            }).catch(function (err) {
                console.log("error: ");
                console.log(err);
            });
        };
    }]);
})();
