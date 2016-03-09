(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', '$timeout', function($scope, FilePost, $timeout) {
        $scope.myFile = null;
        $scope.fileName = "Your PDF File";
        $scope.questions = null;

        $scope.question = {};

        // query pagination settings
        $scope.pag = {
            currentPage: 1,
            maxSize: 10,
            length: 1
        };

        $scope.update = function(questionNumber) {
            var question = $scope.questions.body[questionNumber];
            $scope.question.answer = question.answer;
            $scope.question.answers = question.answers;
            $scope.question.question = question.question;
        };

        $scope.checkAnswer = function(answerNumber) {
            if (answerNumber == $scope.question.answer) {
                console.log('correct answer');
            } else {
                console.log('incorrect answer');
            }
        };

        $scope.uploadFile = function(){
            var file = $scope.myFile;
            console.log("file recived");
            console.log(file);
            var fd = new FormData();
            fd.append('uploadedFile', file);

            FilePost.create({}, fd).$promise.then(function(res){
                $scope.questions = res;

                $scope.question.answer = res.body[0].answer;
                $scope.question.answers = res.body[0].answers;
                $scope.question.question = res.body[0].question;

                $scope.pag.numfound = res.body.length;
                $scope.pag.currentPage = 1;
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
