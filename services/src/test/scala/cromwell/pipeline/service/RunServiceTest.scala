package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dao.repository.RunRepository
import cromwell.pipeline.datastorage.dao.utils.{ TestProjectUtils, TestRunUtils, TestUserUtils }
import cromwell.pipeline.datastorage.dto.{ Done, Run, RunCreateRequest, RunUpdateRequest }
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{ AsyncWordSpec, Matchers }
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class RunServiceTest extends AsyncWordSpec with Matchers with MockitoSugar {

  private val runRepository: RunRepository = mock[RunRepository]
  private val projectService: ProjectService = mock[ProjectService]
  private val runService: RunService = RunService(runRepository, projectService)

  "RunService" when {
    "addRun" should {

      "returns run id" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val project = TestProjectUtils.getDummyProject()
        val projectId = project.projectId
        val userId = TestUserUtils.getDummyUserId
        val run = TestRunUtils.getDummyRun(runId = runId, projectId = projectId, userId = userId)
        val runCreateRequest = RunCreateRequest(
          projectId = projectId,
          projectVersion = run.projectVersion,
          results = run.results
        )

        when(projectService.getUserProjectById(projectId, userId)).thenReturn(Future.successful(project))
        when(runRepository.addRun(any[Run])).thenReturn(Future.successful(runId))
        runService.addRun(runCreateRequest, userId).map {
          _ shouldBe runId
        }
      }
    }

    "getRunById" should {

      "returns run entity if the entity exists" taggedAs Service in {

        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))

        runService.getRunByIdAndUser(runId, run.userId).map { _ shouldBe Some(run) }
      }

      "returns none if the run is not found" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val userId = TestUserUtils.getDummyUserId
        when(runRepository.getRunByIdAndUser(runId, userId)).thenReturn(Future(None))
        runService.getRunByIdAndUser(runId, userId).map { _ shouldBe None }
      }
    }

    "deleteRunById" should {

      "returns 1 if the entity was deleted" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.deleteRunById(runId)).thenReturn(Future.successful(1))

        runService.deleteRunById(runId, run.userId).map { _ shouldBe 1 }
      }
    }

    "updateRun" should {

      "returns the entity updated" taggedAs Service in {
        val runId = TestRunUtils.getDummyRunId
        val run = TestRunUtils.getDummyRun(runId)
        val runUpdated = run.copy(
          status = Done,
          timeStart = run.timeStart,
          timeEnd = TestRunUtils.getDummyTimeEnd(false),
          results = "new-results",
          cmwlWorkflowId = TestRunUtils.getDummyCmwlWorkflowId(false)
        )

        val request = RunUpdateRequest(
          runUpdated.status,
          runUpdated.timeStart,
          runUpdated.timeEnd,
          runUpdated.results,
          runUpdated.cmwlWorkflowId
        )

        when(runRepository.getRunByIdAndUser(runId, run.userId)).thenReturn(Future.successful(Some(run)))
        when(runRepository.updateRun(runUpdated)).thenReturn(Future.successful(1))
        runService.updateRun(runId, request, run.userId).map { _ shouldBe 1 }
      }
    }
  }
}
