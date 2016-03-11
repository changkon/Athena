(function() {
    var app = angular.module('athena');

    app.controller('DashboardQuestionCtrl', ['$scope','FilePost', '$timeout', function($scope, FilePost, $timeout) {
        $scope.showQuestions = false;
        $scope.myFile = null;
        $scope.fileName = "Your PDF File";
        $scope.questions = null;
        $scope.question = {};
        $scope.question.question = "No question currently";
        $scope.answerOnce = false;
        // query pagination settings
        $scope.pag = {
            currentPage: 1,
            maxSize: 10,
            length: 1
        };

        $scope.update = function(questionNumber) {
             $scope.answerOnce = false;
            for (i=0; i< $scope.question.answers.length; i++) {
                var element = angular.element( document.querySelector( '#answer-' + i ) );
                element.removeClass('correct');
                element.removeClass('incorrect');
            }
            var question = $scope.questions.body[questionNumber-1];
            $scope.question.answer = question.answer;
            $scope.question.answers = question.answers;
            $scope.question.question = question.question.replace(/\\n|:\\n/g,"");
        };

        $scope.checkAnswer = function(answerNumber) {
            if (!$scope.answerOnce) {
                var correctElement = angular.element( document.querySelector( '#answer-' + $scope.question.answer ) );
                if (answerNumber == $scope.question.answer) {
                    console.log('correct answer');
                } else {
                    console.log('incorrect answer');
                    var incorrectElement = angular.element( document.querySelector( '#answer-' + answerNumber ) );
                    incorrectElement.addClass('incorrect');
                }
                correctElement.addClass('correct');
                $scope.answerOnce = true;
            }
        };

        $scope.returnMenu = function() {
            $scope.myFile = null;
            $scope.showQuestions = false;
        }

        $scope.uploadFile = function(){
            var file = $scope.myFile;
            console.log("file recived");
            console.log(file);
            var fd = new FormData();
            fd.append('uploadedFile', file);

            $scope.myPromise = FilePost.create({}, fd).$promise.then(function(res){
                $scope.questions = res;

                $scope.question.answer = res.body[0].answer;
                $scope.question.answers = res.body[0].answers;
                $scope.question.question = res.body[0].question.replace(/\\n|:\\n/g,"");

                $scope.pag.numfound = res.body.length;
                $scope.pag.currentPage = 1;
                console.log(res);
                $scope.showQuestions = true;
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
