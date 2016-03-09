(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', '$timeout', function($scope, FilePost, $timeout) {
        $scope.myFile = null;
        $scope.fileName = "Your PDF File";

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

        $scope.openFileModal = function() {
            var inputFileElement = document.getElementById('inputFileElement');
            $timeout(function() {
                inputFileElement.click();
            }, 0);
        };
    }]);
})();
