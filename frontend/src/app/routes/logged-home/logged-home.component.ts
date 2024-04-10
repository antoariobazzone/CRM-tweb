import { UserService } from "../../Services/user.service"
import { UserData } from "../../types/UserTypes"
import { LayoutSingleComponent } from "../../Components/layout-single/layout-single.component"
import { ROLE } from "../../types"
import { Component, OnInit } from "@angular/core"
import { CommonModule, NgOptimizedImage } from "@angular/common"
import { MatButtonModule } from "@angular/material/button"
import { MatIconModule } from "@angular/material/icon"
import { MatListModule } from "@angular/material/list"
import { MatSidenavModule } from "@angular/material/sidenav"
import { MatToolbarModule } from "@angular/material/toolbar"
import { RouterLink, RouterOutlet } from "@angular/router"
import { Employee } from "../../types/data"
import { ProjectComponent } from "../project/project.component"

@Component({
  selector: "app-logged-home",
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterOutlet,
    RouterLink,
    LayoutSingleComponent,
    NgOptimizedImage,
    ProjectComponent,
  ],
  templateUrl: "./logged-home.component.html",
  styleUrl: "./logged-home.component.scss",
})
export class LoggedHomeComponent implements OnInit {
  user: Employee | null = null

  constructor(private userService: UserService) {}

  ngOnInit() {
    this.userService.currUser.subscribe((usr) => {
      if (usr) this.user = usr
    })
  }

  protected readonly ROLE = ROLE
}
